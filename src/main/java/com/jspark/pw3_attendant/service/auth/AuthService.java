package com.jspark.pw3_attendant.service.auth;

import com.jspark.pw3_attendant.common.exception.ApiException;
import com.jspark.pw3_attendant.domain.admin.AdminAccount;
import com.jspark.pw3_attendant.domain.admin.AdminRefreshToken;
import com.jspark.pw3_attendant.domain.admin.ApprovalStatus;
import com.jspark.pw3_attendant.repository.admin.AdminAccountRepository;
import com.jspark.pw3_attendant.repository.admin.AdminRefreshTokenRepository;
import com.jspark.pw3_attendant.service.auth.JwtTokenService.IssuedToken;
import com.jspark.pw3_attendant.service.auth.dto.AdminAccountResponse;
import com.jspark.pw3_attendant.service.auth.dto.LoginRequest;
import com.jspark.pw3_attendant.service.auth.dto.SignupRequest;
import com.jspark.pw3_attendant.service.auth.dto.TokenResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminAccountRepository adminAccountRepository;
    private final AdminRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Transactional
    public AdminAccountResponse signup(SignupRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        if (adminAccountRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", "이미 사용 중인 아이디입니다.");
        }
        if (adminAccountRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다.");
        }

        AdminAccount account = AdminAccount.pending(
                username,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                email,
                normalizePhone(request.phone())
        );
        try {
            return AdminAccountResponse.from(adminAccountRepository.saveAndFlush(account));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "USERNAME_OR_EMAIL_ALREADY_EXISTS",
                    "이미 사용 중인 아이디 또는 이메일입니다."
            );
        }
    }

    @Transactional
    public AdminAccountResponse resubmit(SignupRequest request) {
        String username = normalizeUsername(request.username());
        AdminAccount account = adminAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND", "관리자 계정을 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (account.getApprovalStatus() != ApprovalStatus.REJECTED) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_REJECTED_ACCOUNT", "거절된 계정만 재신청할 수 있습니다.");
        }
        String email = normalizeEmail(request.email());
        if (adminAccountRepository.existsByEmailAndIdNot(email, account.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다.");
        }
        account.resubmit(request.name().trim(), email, normalizePhone(request.phone()));
        return AdminAccountResponse.from(account);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String username = normalizeUsername(request.username());
        AdminAccount account = adminAccountRepository.findByUsername(username)
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw invalidCredentials();
        }
        requireApproved(account);

        account.recordLogin();
        return issueTokenPair(account, trimToNull(request.deviceInfo()));
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue, String deviceInfo) {
        Jwt jwt = jwtTokenService.decodeRefreshToken(refreshTokenValue);
        AdminRefreshToken storedToken = refreshTokenRepository.findByTokenHash(hash(refreshTokenValue))
                .orElseThrow(this::invalidRefreshToken);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (!storedToken.isUsable(now)
                || !storedToken.getAdmin().getId().toString().equals(jwt.getSubject())) {
            throw invalidRefreshToken();
        }
        AdminAccount account = storedToken.getAdmin();
        if (account.getApprovalStatus() != ApprovalStatus.APPROVED) {
            refreshTokenRepository.revokeAllByAdminId(account.getId(), now);
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_NOT_APPROVED", "승인된 관리자만 로그인할 수 있습니다.");
        }

        storedToken.revoke(now);
        String resolvedDeviceInfo = trimToNull(deviceInfo);
        if (resolvedDeviceInfo == null) {
            resolvedDeviceInfo = storedToken.getDeviceInfo();
        }
        return issueTokenPair(account, resolvedDeviceInfo);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByTokenHash(hash(refreshTokenValue))
                .ifPresent(token -> token.revoke(LocalDateTime.now(ZoneOffset.UTC)));
    }

    @Transactional
    public void logoutAll(Long adminId) {
        requireAccount(adminId);
        refreshTokenRepository.revokeAllByAdminId(adminId, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Transactional(readOnly = true)
    public AdminAccountResponse getMe(Long adminId) {
        return AdminAccountResponse.from(requireAccount(adminId));
    }

    private TokenResponse issueTokenPair(AdminAccount account, String deviceInfo) {
        IssuedToken accessToken = jwtTokenService.issueAccessToken(account);
        IssuedToken refreshToken = jwtTokenService.issueRefreshToken(account);
        AdminRefreshToken storedToken = new AdminRefreshToken(
                account,
                hash(refreshToken.value()),
                LocalDateTime.ofInstant(refreshToken.expiresAt(), ZoneOffset.UTC),
                deviceInfo
        );
        refreshTokenRepository.save(storedToken);
        return new TokenResponse(
                "Bearer",
                accessToken.value(),
                accessToken.expiresAt(),
                refreshToken.value(),
                refreshToken.expiresAt(),
                AdminAccountResponse.from(account)
        );
    }

    private AdminAccount requireAccount(Long adminId) {
        return adminAccountRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND", "관리자 계정을 찾을 수 없습니다."));
    }

    private void requireApproved(AdminAccount account) {
        if (account.getApprovalStatus() == ApprovalStatus.PENDING) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_APPROVAL_PENDING", "슈퍼어드민의 승인을 기다리고 있습니다.");
        }
        if (account.getApprovalStatus() == ApprovalStatus.REJECTED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_APPROVAL_REJECTED", "관리자 가입 승인이 거절되었습니다.");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        String normalized = phone.replaceAll("[^0-9]", "");
        if (normalized.length() < 9 || normalized.length() > 15) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PHONE", "연락처는 숫자 9~15자리여야 합니다.");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다.");
    }
}

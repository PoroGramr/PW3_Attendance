package com.jspark.pw3_attendant.service.auth;

import com.jspark.pw3_attendant.common.config.AccountRecoveryProperties;
import com.jspark.pw3_attendant.common.exception.ApiException;
import com.jspark.pw3_attendant.domain.admin.AdminAccount;
import com.jspark.pw3_attendant.domain.admin.AdminAccountRecovery;
import com.jspark.pw3_attendant.domain.admin.RecoveryChannel;
import com.jspark.pw3_attendant.domain.admin.RecoveryPurpose;
import com.jspark.pw3_attendant.repository.admin.AdminAccountRecoveryRepository;
import com.jspark.pw3_attendant.repository.admin.AdminAccountRepository;
import com.jspark.pw3_attendant.repository.admin.AdminRefreshTokenRepository;
import com.jspark.pw3_attendant.service.auth.dto.PasswordResetConfirmRequest;
import com.jspark.pw3_attendant.service.auth.dto.PasswordResetRequest;
import com.jspark.pw3_attendant.service.auth.dto.PasswordResetRequestResponse;
import com.jspark.pw3_attendant.service.auth.dto.RecoveryAcceptedResponse;
import com.jspark.pw3_attendant.service.auth.dto.UsernameReminderRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final String ACCEPTED_MESSAGE =
            "입력한 정보와 일치하는 계정이 있으면 등록된 수신처로 안내를 발송했습니다.";
    private static final String RESET_COMPLETED_MESSAGE = "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.";

    private final AdminAccountRepository adminAccountRepository;
    private final AdminAccountRecoveryRepository recoveryRepository;
    private final AdminRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRecoveryNotifier notifier;
    private final AccountRecoveryProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RecoveryAcceptedResponse sendUsernameReminder(UsernameReminderRequest request) {
        String name = request.name().trim();
        String destination = normalizeDestination(request.channel(), request.destination());
        List<AdminAccount> accounts = findAccountsForUsernameReminder(name, request.channel(), destination);
        if (accounts.isEmpty() || isRateLimited(accounts, RecoveryPurpose.USERNAME_REMINDER)) {
            return accepted();
        }

        LocalDateTime now = LocalDateTime.now();
        AdminAccountRecovery audit = recoveryRepository.saveAndFlush(AdminAccountRecovery.usernameReminder(
                accounts.get(0),
                request.channel(),
                passwordEncoder.encode(UUID.randomUUID().toString()),
                now
        ));
        String usernames = accounts.stream()
                .map(AdminAccount::getUsername)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        String content = "[PW3] 가입 아이디 안내\n아이디: " + usernames;
        if (!sendSafely(request.channel(), destination, "[PW3] 관리자 아이디 안내", content)) {
            recoveryRepository.delete(audit);
        }
        return accepted();
    }

    public PasswordResetRequestResponse requestPasswordReset(PasswordResetRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String destination = normalizeDestination(request.channel(), request.destination());
        Optional<AdminAccount> matchedAccount = adminAccountRepository
                .findByUsername(normalizeUsername(request.username()))
                .filter(account -> destinationMatches(account, request.channel(), destination));
        if (matchedAccount.isEmpty()) {
            return fakeResetResponse(now);
        }

        AdminAccount account = matchedAccount.get();
        Optional<AdminAccountRecovery> latest = recoveryRepository
                .findFirstByAdminIdAndPurposeOrderByCreatedAtDesc(account.getId(), RecoveryPurpose.PASSWORD_RESET);
        if (latest.isPresent()
                && latest.get().getCreatedAt().isAfter(now.minus(properties.resendInterval()))
                && latest.get().isUsable(now, properties.maxAttempts())) {
            return response(latest.get());
        }
        latest.filter(recovery -> recovery.isUsable(now, properties.maxAttempts()))
                .ifPresent(recovery -> {
                    recovery.consume(now);
                    recoveryRepository.save(recovery);
                });

        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        AdminAccountRecovery recovery = recoveryRepository.saveAndFlush(AdminAccountRecovery.passwordReset(
                account,
                request.channel(),
                passwordEncoder.encode(code),
                now.plus(properties.codeValidity())
        ));
        String minutes = String.valueOf(Math.max(1, properties.codeValidity().toMinutes()));
        String content = "[PW3] 비밀번호 초기화 인증번호: " + code
                + "\n" + minutes + "분 안에 입력해 주세요.";
        if (!sendSafely(request.channel(), destination, "[PW3] 비밀번호 초기화 인증번호", content)) {
            recoveryRepository.delete(recovery);
            return fakeResetResponse(now);
        }
        return response(recovery);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public RecoveryAcceptedResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        LocalDateTime now = LocalDateTime.now();
        AdminAccountRecovery recovery = recoveryRepository.findByPublicIdForUpdate(request.requestId())
                .orElseThrow(this::invalidCode);
        if (recovery.getPurpose() != RecoveryPurpose.PASSWORD_RESET
                || !recovery.isUsable(now, properties.maxAttempts())) {
            throw invalidCode();
        }
        if (!passwordEncoder.matches(request.verificationCode(), recovery.getCodeHash())) {
            recovery.registerFailure(properties.maxAttempts(), now);
            throw invalidCode();
        }

        AdminAccount account = recovery.getAdmin();
        account.changePassword(passwordEncoder.encode(request.newPassword()));
        recovery.consume(now);
        adminAccountRepository.saveAndFlush(account);
        recoveryRepository.saveAndFlush(recovery);
        refreshTokenRepository.revokeAllByAdminId(account.getId(), now);
        return new RecoveryAcceptedResponse(RESET_COMPLETED_MESSAGE);
    }

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void cleanupExpiredRequests() {
        recoveryRepository.deleteAllByExpiresAtBefore(LocalDateTime.now().minusDays(7));
    }

    private List<AdminAccount> findAccountsForUsernameReminder(
            String name,
            RecoveryChannel channel,
            String destination) {
        if (channel == RecoveryChannel.EMAIL) {
            return adminAccountRepository.findByEmail(destination)
                    .filter(account -> account.getName().equals(name))
                    .stream()
                    .toList();
        }
        return adminAccountRepository.findAllByPhone(destination).stream()
                .filter(account -> account.getName().equals(name))
                .sorted(Comparator.comparing(AdminAccount::getId))
                .toList();
    }

    private boolean isRateLimited(List<AdminAccount> accounts, RecoveryPurpose purpose) {
        LocalDateTime threshold = LocalDateTime.now().minus(properties.resendInterval());
        return accounts.stream().anyMatch(account -> recoveryRepository
                .existsByAdminIdAndPurposeAndCreatedAtAfter(account.getId(), purpose, threshold));
    }

    private boolean destinationMatches(
            AdminAccount account,
            RecoveryChannel channel,
            String destination) {
        if (channel == RecoveryChannel.EMAIL) {
            return account.getEmail().equals(destination);
        }
        return account.getPhone().equals(destination);
    }

    private String normalizeDestination(RecoveryChannel channel, String destination) {
        if (channel == RecoveryChannel.EMAIL) {
            String email = destination.trim().toLowerCase(Locale.ROOT);
            if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RECOVERY_DESTINATION", "이메일 형식이 올바르지 않습니다.");
            }
            return email;
        }
        String phone = destination.replaceAll("[^0-9]", "");
        if (phone.length() < 9 || phone.length() > 15) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RECOVERY_DESTINATION", "연락처는 숫자 9~15자리여야 합니다.");
        }
        return phone;
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private boolean sendSafely(RecoveryChannel channel, String destination, String subject, String content) {
        try {
            notifier.send(channel, destination, subject, content);
            return true;
        } catch (RuntimeException exception) {
            log.error("Account recovery notification delivery failed: channel={}", channel, exception);
            return false;
        }
    }

    private RecoveryAcceptedResponse accepted() {
        return new RecoveryAcceptedResponse(ACCEPTED_MESSAGE);
    }

    private PasswordResetRequestResponse response(AdminAccountRecovery recovery) {
        return new PasswordResetRequestResponse(
                recovery.getPublicId(),
                recovery.getExpiresAt(),
                ACCEPTED_MESSAGE
        );
    }

    private PasswordResetRequestResponse fakeResetResponse(LocalDateTime now) {
        return new PasswordResetRequestResponse(
                UUID.randomUUID().toString(),
                now.plus(properties.codeValidity()),
                ACCEPTED_MESSAGE
        );
    }

    private ApiException invalidCode() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_OR_EXPIRED_RECOVERY_CODE",
                "인증번호가 올바르지 않거나 만료되었습니다."
        );
    }
}

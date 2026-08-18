package com.jspark.pw3_attendant.service.auth;

import com.jspark.pw3_attendant.common.config.JwtProperties;
import com.jspark.pw3_attendant.common.exception.ApiException;
import com.jspark.pw3_attendant.domain.admin.AdminAccount;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties properties;

    public IssuedToken issueAccessToken(AdminAccount account) {
        return issue(account, ACCESS, properties.accessTokenValidity());
    }

    public IssuedToken issueRefreshToken(AdminAccount account) {
        return issue(account, REFRESH, properties.refreshTokenValidity());
    }

    public Jwt decodeRefreshToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            if (!REFRESH.equals(jwt.getClaimAsString("type"))) {
                throw invalidRefreshToken();
            }
            return jwt;
        } catch (JwtException | IllegalArgumentException exception) {
            throw invalidRefreshToken();
        }
    }

    private IssuedToken issue(AdminAccount account, String type, java.time.Duration validity) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(validity);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(account.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim("username", account.getUsername())
                .claim("role", account.getRole().name())
                .claim("type", type)
                .build();
        JwsHeader header = JwsHeader.with(() -> JwsAlgorithms.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다.");
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}

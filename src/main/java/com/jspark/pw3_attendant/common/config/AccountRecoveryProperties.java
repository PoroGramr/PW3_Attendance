package com.jspark.pw3_attendant.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.account-recovery")
public record AccountRecoveryProperties(
        @NotNull Duration codeValidity,
        @NotNull Duration resendInterval,
        @Min(1) @Max(10) int maxAttempts,
        @NotBlank String mailFrom
) {
    public AccountRecoveryProperties {
        if (codeValidity != null && (codeValidity.isZero() || codeValidity.isNegative())) {
            throw new IllegalArgumentException("app.account-recovery.code-validity must be positive");
        }
        if (resendInterval != null && resendInterval.isNegative()) {
            throw new IllegalArgumentException("app.account-recovery.resend-interval must not be negative");
        }
    }
}

package com.jspark.pw3_attendant.service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

public record AdminPasswordResetRequest(
        @Schema(description = "초기화할 새 비밀번호 (8~72자, UTF-8 최대 72바이트)", example = "new-password")
        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword
) {
    @AssertTrue(message = "새 비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.")
    @Schema(hidden = true)
    public boolean isPasswordWithinBcryptLimit() {
        return newPassword == null || newPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override
    public String toString() {
        return "AdminPasswordResetRequest[newPassword=REDACTED]";
    }
}

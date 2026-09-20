package com.jspark.pw3_attendant.service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @Schema(description = "비밀번호 초기화 요청 ID")
        @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "요청 ID 형식이 올바르지 않습니다.")
        String requestId,

        @Schema(description = "이메일 또는 문자로 받은 6자리 인증번호", example = "123456")
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 숫자 6자리여야 합니다.")
        String verificationCode,

        @Schema(description = "새 비밀번호", example = "new-password")
        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword
) {
}

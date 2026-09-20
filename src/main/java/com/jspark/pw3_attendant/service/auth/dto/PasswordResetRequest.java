package com.jspark.pw3_attendant.service.auth.dto;

import com.jspark.pw3_attendant.domain.admin.RecoveryChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @Schema(description = "관리자 아이디", example = "admin01")
        @NotBlank
        @Size(min = 4, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "아이디 형식이 올바르지 않습니다.")
        String username,

        @Schema(description = "인증번호 수신 방법", example = "SMS")
        @NotNull
        RecoveryChannel channel,

        @Schema(description = "가입된 이메일 또는 전화번호", example = "010-1234-5678")
        @NotBlank
        @Size(max = 254)
        String destination
) {
}

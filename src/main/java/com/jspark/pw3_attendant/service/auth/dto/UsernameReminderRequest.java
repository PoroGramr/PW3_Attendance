package com.jspark.pw3_attendant.service.auth.dto;

import com.jspark.pw3_attendant.domain.admin.RecoveryChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsernameReminderRequest(
        @Schema(description = "가입 시 입력한 이름", example = "홍길동")
        @NotBlank
        @Size(max = 50)
        String name,

        @Schema(description = "수신 방법", example = "EMAIL")
        @NotNull
        RecoveryChannel channel,

        @Schema(description = "가입된 이메일 또는 전화번호", example = "admin@example.com")
        @NotBlank
        @Size(max = 254)
        String destination
) {
}

package com.jspark.pw3_attendant.service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @Schema(description = "로그인 또는 직전 재발급에서 받은 Refresh Token")
        @NotBlank String refreshToken,
        @Schema(description = "선택적인 기기 식별 정보", example = "Chrome on macOS")
        @Size(max = 255) String deviceInfo
) {
}

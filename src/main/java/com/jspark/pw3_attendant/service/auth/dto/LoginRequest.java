package com.jspark.pw3_attendant.service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(description = "관리자 로그인 아이디", example = "admin01")
        @NotBlank String username,
        @Schema(description = "관리자 비밀번호", example = "admin-password")
        @NotBlank String password,
        @Schema(description = "선택적인 기기 식별 정보", example = "Chrome on macOS")
        @Size(max = 255) String deviceInfo
) {
}

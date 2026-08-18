package com.jspark.pw3_attendant.service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @Schema(description = "폐기할 Refresh Token")
        @NotBlank String refreshToken
) {
}

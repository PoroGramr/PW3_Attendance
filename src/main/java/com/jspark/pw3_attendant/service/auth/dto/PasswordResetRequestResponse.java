package com.jspark.pw3_attendant.service.auth.dto;

import java.time.LocalDateTime;

public record PasswordResetRequestResponse(
        String requestId,
        LocalDateTime expiresAt,
        String message
) {
}

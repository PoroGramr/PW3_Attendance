package com.jspark.pw3_attendant.service.auth;

import java.time.LocalDateTime;

/** Immutable notification snapshot; never includes password or hash. */
public record AdminSignupRequested(
        Long accountId, String username, String name, String email, String phone,
        LocalDateTime requestedAt) {
}

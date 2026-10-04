package com.jspark.pw3_attendant.service.integration.acts29.dto;

import java.time.LocalDate;

public record Acts29MatchIssue(
        Long studentId,
        String studentName,
        LocalDate birth,
        String reason
) {
}

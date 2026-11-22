package com.jspark.pw3_attendant.service.integration.acts29.dto;

/**
 * Acts29에는 출석(Y)인데 로컬은 출석이 아니어서 로컬 출석을 지각으로 맞춘 학생.
 */
public record Acts29LocalAttendanceUpdate(
        Long studentId,
        String studentName,
        String previousStatus,
        String nextStatus
) {
}

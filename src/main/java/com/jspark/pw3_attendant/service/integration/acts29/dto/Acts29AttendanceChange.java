package com.jspark.pw3_attendant.service.integration.acts29.dto;

public record Acts29AttendanceChange(
        Long studentId,
        String studentName,
        String localStatus,
        String previousAttendance,
        String nextAttendance,
        String targetUserId
) {
}

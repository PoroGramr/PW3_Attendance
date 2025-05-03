package com.jspark.pw3_attendant.service.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentAttendanceResponse {
    private Long studentId;
    private String studentName;
    private String attendanceStatus; // ATTEND, ABSENT, LATE, OTHER, UNCHECKED
}

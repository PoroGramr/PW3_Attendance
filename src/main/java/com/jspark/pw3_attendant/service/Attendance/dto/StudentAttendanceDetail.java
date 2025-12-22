package com.jspark.pw3_attendant.service.Attendance.dto;

import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudentAttendanceDetail {
    private final Long studentClassId; // 추가
    private final Long studentId;
    private final String studentName;
    private final AttendanceStatus status;
    private final LocalDateTime updatedAt;
}

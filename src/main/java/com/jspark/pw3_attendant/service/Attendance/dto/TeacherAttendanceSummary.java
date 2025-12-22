package com.jspark.pw3_attendant.service.Attendance.dto;

import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeacherAttendanceSummary {
    private final Long teacherId;
    private final String teacherName;
    private final AttendanceStatus status;
    private final LocalDateTime updatedAt;
}

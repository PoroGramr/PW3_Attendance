package com.jspark.pw3_attendant.service.Attendance.dto;

import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentAttendanceStatusDto {
    private Long studentClassId;
    private String studentName;
    private AttendanceStatus status;
}

package com.jspark.pw3_attendant.service.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GradeAttendanceRateDto {
    private String gradeName; // "중1", "중2", "고1" 등
    private double attendanceRate;
    private int totalStudents;
    private int attendedCount;
}

package com.jspark.pw3_attendant.service.Attendance.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WeeklyClassAttendanceDto {
    private LocalDate date;
    private int totalStudents;
    private int attendedCount;
    private double attendanceRate;
}

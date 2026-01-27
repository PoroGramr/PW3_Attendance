package com.jspark.pw3_attendant.service.Attendance.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SundayStatDto {
    private LocalDate sunday;
    private long attendedCount;
    private long totalCount;
    private double attendanceRate;
}

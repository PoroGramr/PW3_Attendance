package com.jspark.pw3_attendant.service.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class SundayAttendanceSummaryResponse {
    private LocalDate attendanceDate;
    private long attendedCount;
    private long totalCount;
}

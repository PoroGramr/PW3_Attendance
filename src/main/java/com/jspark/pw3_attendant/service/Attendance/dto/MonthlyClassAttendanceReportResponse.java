package com.jspark.pw3_attendant.service.Attendance.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MonthlyClassAttendanceReportResponse {
    private int year;
    private int month;
    private int schoolYear;
    private int totalSundays;
    private List<LocalDate> sundays;
    private double averageAttendanceRate;
    private double weakClassThreshold;
    private int totalClasses;
    private int weakClassCount;
    private List<MonthlyClassAttendanceDto> topClasses;
    private List<MonthlyClassAttendanceDto> classes;
    private List<WeakClassDto> weakClasses;
}

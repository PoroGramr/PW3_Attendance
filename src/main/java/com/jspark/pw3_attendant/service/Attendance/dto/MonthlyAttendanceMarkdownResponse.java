package com.jspark.pw3_attendant.service.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MonthlyAttendanceMarkdownResponse {
    private int year;
    private int month;
    private int schoolYear;
    private String markdown;
    private MonthlyClassAttendanceReportResponse reportData;
}

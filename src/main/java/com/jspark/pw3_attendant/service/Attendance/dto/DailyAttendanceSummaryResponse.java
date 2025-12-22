package com.jspark.pw3_attendant.service.Attendance.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class DailyAttendanceSummaryResponse {
    private final LocalDate date;
    private final int schoolYear;
    private final List<ClassDetailedAttendanceResponse> classAttendances;
    private final List<TeacherAttendanceSummary> teacherAttendances;

    public DailyAttendanceSummaryResponse(LocalDate date, int schoolYear, List<ClassDetailedAttendanceResponse> classAttendances, List<TeacherAttendanceSummary> teacherAttendances) {
        this.date = date;
        this.schoolYear = schoolYear;
        this.classAttendances = classAttendances;
        this.teacherAttendances = teacherAttendances;
    }
}

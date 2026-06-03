package com.jspark.pw3_attendant.service.Attendance.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MonthlyClassAttendanceDto {
    private Long classRoomId;
    private String className;
    private String teacherName;
    private int totalStudents;
    private double averageAttendedCount;
    private double attendanceRate;
    private Double previousMonthAttendanceRate;
    private Double monthOverMonthChange;
    private int rank;
    private String status;
    private List<WeeklyClassAttendanceDto> weeklyStats;
}

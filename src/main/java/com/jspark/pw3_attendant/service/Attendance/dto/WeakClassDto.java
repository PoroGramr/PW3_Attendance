package com.jspark.pw3_attendant.service.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WeakClassDto {
    private Long classRoomId;
    private String className;
    private String teacherName;
    private double attendanceRate;
    private Double previousMonthAttendanceRate;
    private Double monthOverMonthChange;
    private String reason;
}

package com.jspark.pw3_attendant.service.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParentAttendanceStatsResponse {

    private int totalStudents; // 전체 재학생 수
    private int studentsWithParent; // 부/모 중 1명이라도 출석한 학생 수
    private int totalParentsAttended; // 출석한 부모 총 인원 (부 출석 수 + 모 출석 수)
}

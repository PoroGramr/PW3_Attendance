package com.jspark.pw3_attendant.service.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StudentNeedsCareDto {
    private Long studentId;
    private String studentName;
    private String className;
    private int absenceCount;
    private int lateCount;
    private String reason; // "결석 2회 이상" 또는 "지각 3회 이상"
}

package com.jspark.pw3_attendant.service.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AbsenteeResponse {
    private Long studentId;
    private String studentName;
    private String className;
    private String studentPhone;
    private String parentPhone;
}

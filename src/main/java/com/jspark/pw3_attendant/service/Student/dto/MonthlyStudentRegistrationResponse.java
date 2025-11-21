package com.jspark.pw3_attendant.service.Student.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MonthlyStudentRegistrationResponse {
    private int month;
    private List<StudentInfo> students;
}

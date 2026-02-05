package com.jspark.pw3_attendant.service.birthday.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlyBirthdayResponse {
    private int month;
    private List<StudentBirthdayResponse> students;
    private List<TeacherBirthdayResponse> teachers;
}
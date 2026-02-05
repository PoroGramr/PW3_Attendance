package com.jspark.pw3_attendant.service.birthday.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentBirthdayResponse {
    private Long id;
    private String name;
    private LocalDate birth;
    private String className;
    private String phone;
}
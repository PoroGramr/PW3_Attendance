package com.jspark.pw3_attendant.service.birthday.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeacherBirthdayResponse {
    private Long id;
    private String name;
    private LocalDate birth;
    private String phone;
}

package com.jspark.pw3_attendant.service.Teacher.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeacherRequest {
    private String name;
    private LocalDate birth;
    private String phone;

}

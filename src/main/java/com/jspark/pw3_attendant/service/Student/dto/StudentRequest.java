package com.jspark.pw3_attendant.service.Student.dto;

import com.jspark.pw3_attendant.domain.Student.Student.Sex;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudentRequest {
    private String name;
    private LocalDate birth;
    private Sex sex;
    private String phone;
    private String parentPhone;
    private String school;
    private String memo;;
}

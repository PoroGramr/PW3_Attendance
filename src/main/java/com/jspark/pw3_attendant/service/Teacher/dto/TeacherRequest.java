package com.jspark.pw3_attendant.service.Teacher.dto;

import com.jspark.pw3_attendant.domain.Teacher.Teacher.Sex;
import com.jspark.pw3_attendant.domain.Teacher.Teacher.TeacherType;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeacherRequest {
    private String name;
    private LocalDate birth;
    private Sex sex;
    private String phone;
    private TeacherType teacherType;
    private String memo;
}

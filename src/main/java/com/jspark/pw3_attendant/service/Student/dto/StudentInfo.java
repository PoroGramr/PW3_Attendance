package com.jspark.pw3_attendant.service.Student.dto;

import com.jspark.pw3_attendant.domain.Student.Student;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentInfo {
    private Long id;
    private String name;
    private String birth;
    private String phone;

    public static StudentInfo from(Student student) {
        return StudentInfo.builder()
                .id(student.getId())
                .name(student.getName())
                .phone(student.getPhone())
                .build();
    }
}

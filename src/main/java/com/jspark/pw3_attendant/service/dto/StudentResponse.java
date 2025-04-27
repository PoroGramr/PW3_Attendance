package com.jspark.pw3_attendant.service.dto;

import com.jspark.pw3_attendant.domain.Student;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentResponse {

    private Long id;
    private String name;

    public static StudentResponse from(Student student) {
        return new StudentResponse(
            student.getId(),
            student.getName()
        );
    }
}

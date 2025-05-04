package com.jspark.pw3_attendant.service.Teacher.dto;


import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeacherResponse {

    private Long id;
    private String name;
    private String number;

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
            teacher.getId(),
            teacher.getName(),
            teacher.getPhone()
        );
    }
}

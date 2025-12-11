package com.jspark.pw3_attendant.service.Teacher.dto;


import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.domain.Teacher.Teacher.Sex;
import com.jspark.pw3_attendant.domain.Teacher.Teacher.TeacherType;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeacherResponse {

    private Long id;
    private String name;
    private String number;
    private LocalDate birth;
    private Sex sex;
    private String phone;
    private TeacherType teacherType;
    private String memo;


    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
            teacher.getId(),
            teacher.getName(),
            teacher.getPhone(),
            teacher.getBirth(),
            teacher.getSex(),
            teacher.getPhone(),
            teacher.getTeacherType(),
            teacher.getMemo()
        );
    }
}

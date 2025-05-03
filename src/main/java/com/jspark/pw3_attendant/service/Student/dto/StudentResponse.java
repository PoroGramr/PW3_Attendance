package com.jspark.pw3_attendant.service.Student.dto;


import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentResponse {

    private Long id;
    private String name;
    private LocalDate birth;       // 신규 필드
    private String phone;          // 신규 필드
    private Integer schoolYear;    // 신규 필드
    private Long classRoomId;

    public static StudentResponse from(Student student) {
        return new StudentResponse(
            student.getId(),
            student.getName(),
            null,    // birth
            null,    // phone
            null,    // schoolYear
            null
        );
    }

    // ─ 추가하는, 학년도·반 정보까지 채워주는 맵핑
    public static StudentResponse from(StudentClass sc) {
        Student s = sc.getStudent();
        return new StudentResponse(
            s.getId(),
            s.getName(),
            s.getBirth(),
            s.getPhone(),
            sc.getSchoolYear(),
            sc.getClassRoom().getId()
        );
    }
}

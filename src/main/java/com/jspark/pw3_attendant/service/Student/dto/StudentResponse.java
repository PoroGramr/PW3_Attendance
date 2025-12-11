package com.jspark.pw3_attendant.service.Student.dto;


import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.Student.Student.Sex;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentResponse {

    private Long id;
    private String name;
    private LocalDate birth;
    private Sex sex;
    private String phone;
    private String parentPhone;
    private String school;
    private String memo;
    private LocalDateTime deletedAt;
    private Map<Integer, List<ClassRoomResponse>> classesByYear;

    public static StudentResponse from(Student student, Map<Integer, List<ClassRoomResponse>> classesByYear) {
        return new StudentResponse(
            student.getId(),
            student.getName(),
            student.getBirth(),
            student.getSex(),
            student.getPhone(),
            student.getParentPhone(),
            student.getSchool(),
            student.getMemo(),
            student.getDeletedAt(),
            classesByYear
        );
    }
}

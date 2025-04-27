package com.jspark.pw3_attendant.service.dto;

import com.jspark.pw3_attendant.domain.StudentClass;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentClassResponse {

    private Long id;
    private Long studentId;
    private Long classRoomId;
    private Integer schoolYear;

    public static StudentClassResponse from(StudentClass studentClass) {
        return new StudentClassResponse(
            studentClass.getId(),
            studentClass.getStudent().getId(),
            studentClass.getClassRoom().getId(),
            studentClass.getSchoolYear()
        );
    }
}

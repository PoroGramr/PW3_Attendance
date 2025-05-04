package com.jspark.pw3_attendant.service.StudentClass.dto;


import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentClassSummaryResponse {
    private long id;
    private Long studentId;
    private String studentName;

    public static StudentClassSummaryResponse from(StudentClass sc) {
        return new StudentClassSummaryResponse(
            sc.getId(),
            sc.getStudent().getId(),
            sc.getStudent().getName()
        );
    }
}

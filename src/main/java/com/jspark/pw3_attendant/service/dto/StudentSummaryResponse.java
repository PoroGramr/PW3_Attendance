package com.jspark.pw3_attendant.service.dto;

import com.jspark.pw3_attendant.domain.Student;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentSummaryResponse {
    private Long studentId;
    private String studentName;

    public static StudentSummaryResponse from(Student student) {
        return new StudentSummaryResponse(
            student.getId(),
            student.getName()
        );
    }
}

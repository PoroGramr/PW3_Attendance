package com.jspark.pw3_attendant.service.qr.dto;

import com.jspark.pw3_attendant.domain.Student.Student;
import lombok.Getter;

@Getter
public class QrResolveResponseDto {
    private final StudentInfo student;
    private final String qrPayload;

    public QrResolveResponseDto(Student student, String qrPayload) {
        this.student = new StudentInfo(student);
        this.qrPayload = qrPayload;
    }

    @Getter
    private static class StudentInfo {
        private final Long id;
        private final String name;
        // The user mentioned studentNumber but it is not in the Student entity.
        // I will use the student's ID for now.
        // If a student number is available, it can be added here.

        public StudentInfo(Student student) {
            this.id = student.getId();
            this.name = student.getName();
        }
    }
}

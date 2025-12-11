package com.jspark.pw3_attendant.service.qr.dto;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.student_qr.StudentQr;
import lombok.Getter;

@Getter
public class StudentQrResponseDto {
    private final Long studentId;
    private final String studentName;
    private final String qrSecret;
    private final String qrUrl;

    public StudentQrResponseDto(Student student, StudentQr studentQr, String qrUrlBase) {
        this.studentId = student.getId();
        this.studentName = student.getName();
        this.qrSecret = studentQr.getQrSecret();
        this.qrUrl = String.format("%s/s/%s", qrUrlBase, studentQr.getQrSecret());
    }

    // Constructor for when a QR record does not exist yet.
    public StudentQrResponseDto(Student student) {
        this.studentId = student.getId();
        this.studentName = student.getName();
        this.qrSecret = null;
        this.qrUrl = null;
    }
}

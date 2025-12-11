package com.jspark.pw3_attendant.service.attendance.dto;

import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import com.jspark.pw3_attendant.domain.Student.Student;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ScanResponseDto {
    private final String result;
    private final StudentInfo student;
    private final Attendance.AttendanceStatus status;
    private final LocalDateTime timestamp;

    public ScanResponseDto(String result, Student student, Attendance attendance) {
        this.result = result;
        this.student = new StudentInfo(student);
        this.status = attendance.getStatus();
        this.timestamp = attendance.getUpdatedAt();
    }

    public ScanResponseDto(String result) {
        this.result = result;
        this.student = null;
        this.status = null;
        this.timestamp = LocalDateTime.now();
    }

    @Getter
    private static class StudentInfo {
        private final Long id;
        private final String name;

        public StudentInfo(Student student) {
            this.id = student.getId();
            this.name = student.getName();
        }
    }
}

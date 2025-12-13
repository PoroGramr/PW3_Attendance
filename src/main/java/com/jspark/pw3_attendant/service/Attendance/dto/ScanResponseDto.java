package com.jspark.pw3_attendant.service.Attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import com.jspark.pw3_attendant.domain.Student.Student;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ScanResponseDto {
    private final String result;
    private final StudentInfo student;
    private final AttendanceStatus status;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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

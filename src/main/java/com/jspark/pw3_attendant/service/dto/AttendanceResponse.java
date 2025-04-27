package com.jspark.pw3_attendant.service.dto;

import com.jspark.pw3_attendant.domain.Attendance;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AttendanceResponse {
    private Long studentClassId;
    private LocalDate date;
    private String status;

    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
            attendance.getStudentClass().getId(),
            attendance.getDate(),
            attendance.getStatus().name()
        );
    }
}

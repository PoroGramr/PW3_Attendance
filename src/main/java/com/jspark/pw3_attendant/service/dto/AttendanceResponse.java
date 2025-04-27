package com.jspark.pw3_attendant.service.dto;

import com.jspark.pw3_attendant.domain.Attendance;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttendanceResponse {

    private Long studentId;
    private String status;

    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
            attendance.getStudentClass().getStudent().getId(),
            attendance.getStatus().name()
        );
    }
}

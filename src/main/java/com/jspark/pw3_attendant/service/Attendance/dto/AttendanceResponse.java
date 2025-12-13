package com.jspark.pw3_attendant.service.Attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AttendanceResponse {
    private Long studentClassId;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
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

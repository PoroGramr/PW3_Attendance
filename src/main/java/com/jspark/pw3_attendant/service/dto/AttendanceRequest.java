package com.jspark.pw3_attendant.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class AttendanceRequest {
    private Long studentId;
    private Long classRoomId;
    private Integer schoolYear;
    private LocalDate date;
    private String status; // "ATTEND", "LATE", "ABSENT", "OTHER"
}

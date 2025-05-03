package com.jspark.pw3_attendant.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class AttendanceRequest {
    private Long studentClassId;  // 🔥 학생-반-학년도 매핑 ID
    private LocalDate date;
    private String status;        // "ATTEND", "ABSENT", "LATE", "OTHER"
}

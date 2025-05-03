package com.jspark.pw3_attendant.service.StudentClass.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudentClassRequest {
    private Long studentId;
    private Long classRoomId;
    private Integer schoolYear; // 2025, 2026 이런거
}

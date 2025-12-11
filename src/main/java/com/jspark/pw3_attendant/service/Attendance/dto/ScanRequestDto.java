package com.jspark.pw3_attendant.service.attendance.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScanRequestDto {
    private Long courseId; // Corresponds to classRoomId
    private String qrPayload;
}

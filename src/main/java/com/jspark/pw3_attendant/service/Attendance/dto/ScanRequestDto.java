package com.jspark.pw3_attendant.service.Attendance.dto;

import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScanRequestDto {
    private String qrPayload;
    private Attendance.AttendanceStatus status;
}

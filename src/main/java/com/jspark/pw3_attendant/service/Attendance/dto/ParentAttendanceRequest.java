package com.jspark.pw3_attendant.service.Attendance.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ParentAttendanceRequest {

    private String fatherStatus; // ATTEND or ABSENT
    private String motherStatus; // ATTEND or ABSENT
}

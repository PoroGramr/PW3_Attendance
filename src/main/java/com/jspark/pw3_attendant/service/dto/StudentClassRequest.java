package com.jspark.pw3_attendant.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudentClassRequest {
    private Long studentId;
    private Long classRoomId;
    private Integer schoolYear;
}


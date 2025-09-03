package com.jspark.pw3_attendant.service.TeacherClass.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeacherClassRequest {
    private Long teacherId;
    private Long classRoomId;
    private Integer schoolYear;
}

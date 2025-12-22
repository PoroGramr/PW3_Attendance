package com.jspark.pw3_attendant.service.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ClassDetailedAttendanceResponse {
    private final Long classRoomId;
    private final String classRoomName;
    private final List<StudentAttendanceDetail> students;
}

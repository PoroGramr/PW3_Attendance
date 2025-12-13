package com.jspark.pw3_attendant.service.Attendance.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClassAttendanceResponse {
    private Long classRoomId;
    private String className;
    private String teacherName;
    private List<StudentAttendanceStatusDto> students;
}

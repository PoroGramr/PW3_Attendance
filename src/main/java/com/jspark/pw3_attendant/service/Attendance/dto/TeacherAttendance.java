package com.jspark.pw3_attendant.service.Attendance.dto;

import com.jspark.pw3_attendant.domain.Attendance.AttendanceTeacher;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeacherAttendance {
    private Long teacherId;
    private String teacherName;
    private String attendanceStatus;

    public TeacherAttendance(Long teacherId, String teacherName, String attendanceStatus) {
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.attendanceStatus = attendanceStatus != null ? attendanceStatus : "UNCHECKED"; // 상태가 null이면 "UNCHECKED"
    }
}

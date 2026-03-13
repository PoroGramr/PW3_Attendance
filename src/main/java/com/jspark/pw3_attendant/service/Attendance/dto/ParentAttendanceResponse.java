package com.jspark.pw3_attendant.service.Attendance.dto;

import com.jspark.pw3_attendant.domain.Attendance.ParentAttendance;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParentAttendanceResponse {

    private Long studentId;
    private String studentName;
    private LocalDate date;
    private String fatherStatus;
    private String motherStatus;

    public static ParentAttendanceResponse from(ParentAttendance pa) {
        return new ParentAttendanceResponse(
                pa.getStudent().getId(),
                pa.getStudent().getName(),
                pa.getDate(),
                pa.getFatherStatus().name(),
                pa.getMotherStatus().name());
    }

    /** 미기록 학생용 — 부/모 모두 ABSENT 기본값 */
    public static ParentAttendanceResponse unchecked(com.jspark.pw3_attendant.domain.Student.Student student,
            LocalDate date) {
        return new ParentAttendanceResponse(
                student.getId(),
                student.getName(),
                date,
                "ABSENT",
                "ABSENT");
    }
}

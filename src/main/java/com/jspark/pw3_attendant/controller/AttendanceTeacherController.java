package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.Attendance.AttendanceTeacher;
import com.jspark.pw3_attendant.service.Attendance.AttendanceTeacherService;
import com.jspark.pw3_attendant.service.Attendance.dto.TeacherAttendance;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceTeacherController {

    private final AttendanceTeacherService attendanceTeacherService;

    public AttendanceTeacherController(AttendanceTeacherService attendanceTeacherService) {
        this.attendanceTeacherService = attendanceTeacherService;
    }

    // 선생님 본인 출석 체크
    @PostMapping("/teacher/mark")
    @Operation(summary = "선생님 출석 체크")
    public ResponseEntity<String> markTeacherAttendance(@RequestParam Long teacherId,
        @RequestParam String status, // 출석 상태를 String으로 받음
        @RequestParam LocalDate date) {
        try {
            // status 값에 따라 출석 상태를 설정
            AttendanceTeacher.AttendanceStatus attendanceStatus = AttendanceTeacher.AttendanceStatus.valueOf(status.toUpperCase());
            attendanceTeacherService.markTeacherAttendance(teacherId, attendanceStatus, date);
            return ResponseEntity.ok("선생님 출석 체크 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("잘못된 출석 상태 값입니다. (가능한 값: ATTEND, LATE, ABSENT, OTHER)");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("출석 체크 실패: " + e.getMessage());
        }
    }

    // 선생님 본인 출석 상태 조회
    @GetMapping("/teacher/status")
    @Operation(summary = "선생님 단일 출석 조회")
    public ResponseEntity<AttendanceTeacher> getTeacherAttendanceStatus(@RequestParam Long teacherId,
        @RequestParam LocalDate date) {
        AttendanceTeacher attendance = attendanceTeacherService.getTeacherAttendanceStatus(teacherId, date);
        return ResponseEntity.ok(attendance);
    }
    @Operation(summary = "선생님 전체 출석 조회")
    @GetMapping("/teachers/status")
    public ResponseEntity<List<TeacherAttendance>> getAllTeachersAttendance(@RequestParam LocalDate date) {
        List<TeacherAttendance> attendanceList = attendanceTeacherService.getAllTeachersAttendanceByDate(date);
        return ResponseEntity.ok(attendanceList);
    }
}
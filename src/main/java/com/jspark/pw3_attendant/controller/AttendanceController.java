package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import com.jspark.pw3_attendant.service.Attendance.AttendanceService;

import com.jspark.pw3_attendant.service.Attendance.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping(value = "/report-by-date", produces = "text/plain;charset=UTF-8")
    @Operation(summary = "일별 출석 리포트 텍스트 생성")
    public ResponseEntity<String> getDailyAttendanceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String report = attendanceService.getDailyAttendanceReport(date);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/summary-by-date")
    @Operation(summary = "일별 출석 현황 요약 (반별, 교사별)")
    public ResponseEntity<DailyAttendanceSummaryResponse> getDailyAttendanceSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer schoolYear) {
        DailyAttendanceSummaryResponse response = attendanceService.getDailyAttendanceSummary(date, schoolYear);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary/sundays")
    @Operation(summary = "일요일별 전체 출석 요약 조회")
    public ResponseEntity<List<SundayAttendanceSummaryResponse>> getSundayAttendanceSummary() {
        return ResponseEntity.ok(attendanceService.getSundayAttendanceSummary());
    }

    @GetMapping("/summary/grades/sundays")
    @Operation(summary = "학년별 최근 1달 일요일 출석 요약 조회")
    public ResponseEntity<List<GradeSundayAttendanceResponse>> getGradeSundayAttendanceSummary() {
        return ResponseEntity.ok(attendanceService.getGradeSundayAttendanceSummary());
    }

    @PutMapping("/{studentClassId}/{date}")
    @Operation(summary = "특정 학생, 특정일 출석 데이터 생성, 수정")
    public ResponseEntity<Void> upsertAttendance(
            @PathVariable Long studentClassId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody UpsertAttendanceRequest request) {
        boolean created = attendanceService.upsertAttendance(studentClassId, date,
                AttendanceStatus.valueOf(request.getStatus()));
        return created
                ? ResponseEntity.status(HttpStatus.CREATED).build() // 출석 여부 생성
                : ResponseEntity.ok().build(); // 출석 여부 수정
    }

    @GetMapping("/{studentClassId}")
    @Operation(summary = "특정 학생의 출석 데이터 조회")
    public List<AttendanceResponse> getAttendancesByStudentClass(@PathVariable Long studentClassId) {
        return attendanceService.findByStudentClass(studentClassId).stream()
                .map(AttendanceResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 해당 반 id, 해당 학년도, 해당 일자
     */
    @GetMapping("/year/{schoolYear}/date/{date}")
    @Operation(summary = "특정 학년도, 특정일 학생 전체 출석 여부 조회")
    public ResponseEntity<List<StudentAttendanceResponse>> getYearAttendanceByDate(
            @PathVariable Integer schoolYear,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<StudentAttendanceResponse> list = attendanceService.findYearAttendances(schoolYear, date);
        return ResponseEntity.ok(list);
    }

    /**
     * 해당 반 id, 해당 학년도, 해당 일자
     */
    @GetMapping("/class/{classRoomId}/year/{schoolYear}/date/{date}")
    @Operation(summary = "특정 반, 특정 학년도, 특정일 출석 데이터 조회")
    public ResponseEntity<List<StudentAttendanceResponse>> getClassAttendanceByDate(
            @PathVariable Long classRoomId,
            @PathVariable Integer schoolYear,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<StudentAttendanceResponse> list = attendanceService.findStudentAttendances(classRoomId, schoolYear, date);
        return ResponseEntity.ok(list);
    }

    /**
     * 특정 반(classRoomId)의 해당 날짜(date) 출석 현황 조회
     * GET /attendances/classrooms/{classRoomId}/date/{date}
     */
    @GetMapping("/classrooms/{classRoomId}/date/{date}")
    @Operation(summary = "특정 반, 특정일 출석 데이터 조회")
    public ResponseEntity<List<StudentAttendanceResponse>> getAttendanceByClassAndDate(
            @PathVariable Long classRoomId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<StudentAttendanceResponse> list = attendanceService.findStudentAttendancesByClassAndDate(classRoomId,
                date);

        return ResponseEntity.ok(list);
    }

    @GetMapping("/classrooms/{classRoomId}/sundays/summary")
    @Operation(summary = "특정 반의 일요일별 출석 요약 조회")
    public ResponseEntity<List<ClassSundayAttendanceResponse>> getSundayAttendanceSummaryForClass(
            @PathVariable Long classRoomId) {
        List<ClassSundayAttendanceResponse> list = attendanceService.getSundayAttendanceSummaryForClass(classRoomId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/classes/year/{schoolYear}/date/{date}")
    @Operation(summary = "특정 학년도, 특정일 반별 학생 출석 조회")
    public ResponseEntity<List<ClassAttendanceResponse>> getAttendanceByClassForDateAndYear(
            @PathVariable Integer schoolYear,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ClassAttendanceResponse> list = attendanceService.getAttendanceByClassForDateAndYear(schoolYear, date);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/scan")
    @Operation(summary = "QR 코드 스캔으로 출석 처리 (교사용)")
    // TODO: Add security check to ensure only ADMIN/TEACHER can access this.
    public ResponseEntity<ScanResponseDto> scanAttendance(@RequestBody ScanRequestDto request) {
        try {
            ScanResponseDto response = attendanceService.processScan(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ScanResponseDto(e.getMessage()));
        }
    }
}

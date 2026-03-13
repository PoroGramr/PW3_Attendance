package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.service.Attendance.ParentAttendanceService;
import com.jspark.pw3_attendant.service.Attendance.dto.ParentAttendanceRequest;
import com.jspark.pw3_attendant.service.Attendance.dto.ParentAttendanceResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.ParentAttendanceStatsResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.ParentSingleAttendanceRequest;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parent-attendance")
public class ParentAttendanceController {

    private final ParentAttendanceService parentAttendanceService;

    /**
     * 부모 출석 생성 / 수정 (upsert)
     * PUT /api/parent-attendance/{studentId}/{date}
     */
    @PutMapping("/{studentId}/{date}")
    @Operation(summary = "학생별 부/모 출석 생성 및 수정 (upsert)")
    public ResponseEntity<Void> upsertParentAttendance(
            @PathVariable Long studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody ParentAttendanceRequest request) {
        boolean created = parentAttendanceService.upsert(studentId, date, request);
        return created
                ? ResponseEntity.status(HttpStatus.CREATED).build()
                : ResponseEntity.ok().build();
    }

    /**
     * 특정 학생, 특정 날짜 조회
     * GET /api/parent-attendance/{studentId}/{date}
     */
    @GetMapping("/{studentId}/{date}")
    @Operation(summary = "특정 학생, 특정 날짜 부/모 출석 조회")
    public ResponseEntity<ParentAttendanceResponse> getParentAttendance(
            @PathVariable Long studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(parentAttendanceService.getByStudentAndDate(studentId, date));
    }

    /**
     * 특정 날짜 기록된 학생 목록 조회
     * GET /api/parent-attendance/date/{date}
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "특정 날짜 부/모 출석 기록 전체 조회 (기록 있는 학생만)")
    public ResponseEntity<List<ParentAttendanceResponse>> getParentAttendanceByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(parentAttendanceService.getAllByDate(date));
    }

    /**
     * 특정 날짜 전체 재학생 목록 조회 (미기록 학생은 ABSENT/ABSENT 기본값)
     * GET /api/parent-attendance/date/{date}/all
     */
    @GetMapping("/date/{date}/all")
    @Operation(summary = "특정 날짜 전체 재학생 부/모 출석 조회 (미기록 포함, 기본값 ABSENT)")
    public ResponseEntity<List<ParentAttendanceResponse>> getAllStudentsParentAttendance(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(parentAttendanceService.getAllStudentsWithDefault(date));
    }

    /**
     * 부모 출석 삭제
     * DELETE /api/parent-attendance/{studentId}/{date}
     */
    @DeleteMapping("/{studentId}/{date}")
    @Operation(summary = "특정 학생, 특정 날짜 부/모 출석 삭제")
    public ResponseEntity<Void> deleteParentAttendance(
            @PathVariable Long studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        parentAttendanceService.delete(studentId, date);
        return ResponseEntity.noContent().build();
    }

    /**
     * 부 출석만 단독 생성·수정
     * PUT /api/parent-attendance/{studentId}/{date}/father
     */
    @PutMapping("/{studentId}/{date}/father")
    @Operation(summary = "부(아버지) 출석 단독 생성 및 수정")
    public ResponseEntity<Void> upsertFatherAttendance(
            @PathVariable Long studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody ParentSingleAttendanceRequest request) {
        boolean created = parentAttendanceService.upsertFather(studentId, date, request);
        return created
                ? ResponseEntity.status(HttpStatus.CREATED).build()
                : ResponseEntity.ok().build();
    }

    /**
     * 모 출석만 단독 생성·수정
     * PUT /api/parent-attendance/{studentId}/{date}/mother
     */
    @PutMapping("/{studentId}/{date}/mother")
    @Operation(summary = "모(어머니) 출석 단독 생성 및 수정")
    public ResponseEntity<Void> upsertMotherAttendance(
            @PathVariable Long studentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody ParentSingleAttendanceRequest request) {
        boolean created = parentAttendanceService.upsertMother(studentId, date, request);
        return created
                ? ResponseEntity.status(HttpStatus.CREATED).build()
                : ResponseEntity.ok().build();
    }

    /**
     * 부모 출석 통계
     * GET /api/parent-attendance/date/{date}/stats
     */
    @GetMapping("/date/{date}/stats")
    @Operation(summary = "특정 날짜 부/모 출석 현황 통계")
    public ResponseEntity<ParentAttendanceStatsResponse> getParentAttendanceStats(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(parentAttendanceService.getStats(date));
    }
}

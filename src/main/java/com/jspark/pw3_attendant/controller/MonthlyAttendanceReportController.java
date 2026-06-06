package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.service.Attendance.MonthlyAttendanceReportService;
import com.jspark.pw3_attendant.service.Attendance.dto.MonthlyAttendanceMarkdownResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.MonthlyAttendanceReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports/monthly-attendance")
public class MonthlyAttendanceReportController {

    private final MonthlyAttendanceReportService monthlyAttendanceReportService;

    @GetMapping("/cache")
    @Operation(summary = "월별 반 출석 리포트 캐시 조회", description = "이미 생성되어 로컬 캐시에 저장된 월별 반 출석 리포트를 조회합니다.")
    public ResponseEntity<MonthlyAttendanceMarkdownResponse> getCachedMonthlyAttendanceReport(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int schoolYear,
            @RequestParam(required = false) Double weakClassThreshold) {
        return monthlyAttendanceReportService.getCachedMonthlyReport(year, month, schoolYear, weakClassThreshold)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/generate")
    @Operation(summary = "월별 반 출석 리포트 생성", description = "월별 반 출석률을 집계하고 관리자용 마크다운 리포트를 생성한 뒤 로컬 캐시를 갱신합니다.")
    public ResponseEntity<MonthlyAttendanceMarkdownResponse> generateMonthlyAttendanceReport(
            @Valid @RequestBody MonthlyAttendanceReportRequest request) {
        return ResponseEntity.ok(monthlyAttendanceReportService.generateMonthlyReport(request));
    }
}

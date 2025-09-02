package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import com.jspark.pw3_attendant.service.Attendance.AttendanceService;

import com.jspark.pw3_attendant.service.Attendance.dto.AttendanceResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.StudentAttendanceResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.ClassSundayAttendanceResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.SundayAttendanceSummaryResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.UpsertAttendanceRequest;
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
@RequestMapping("/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/summary/sundays")
    @Operation(summary = "일요일별 전체 출석 요약 조회")
    public ResponseEntity<List<SundayAttendanceSummaryResponse>> getSundayAttendanceSummary() {
        return ResponseEntity.ok(attendanceService.getSundayAttendanceSummary());
    }

    @PutMapping("/{studentClassId}/{date}")
    @Operation(summary = "특정 학생, 특정일 출석 데이터 생성, 수정")
    public ResponseEntity<Void> upsertAttendance(
        @PathVariable Long studentClassId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestBody UpsertAttendanceRequest request
    ) {
        boolean created = attendanceService.upsertAttendance(studentClassId, date, AttendanceStatus.valueOf(request.getStatus()));
        return created
            ? ResponseEntity.status(HttpStatus.CREATED).build()   // 출석 여부 생성
            : ResponseEntity.ok().build();                       // 출석 여부 수정
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
     * */
    @GetMapping("/year/{schoolYear}/date/{date}")
    @Operation(summary = "특정 학년도, 특정일 학생 전체 출석 여부 조회")
    public ResponseEntity<List<StudentAttendanceResponse>> getYearAttendanceByDate(
        @PathVariable Integer schoolYear,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<StudentAttendanceResponse> list =
            attendanceService.findYearAttendances(schoolYear, date);
        return ResponseEntity.ok(list);
    }
    /**
     * 해당 반 id, 해당 학년도, 해당 일자
     * */
    @GetMapping("/class/{classRoomId}/year/{schoolYear}/date/{date}")
    @Operation(summary = "특정 반, 특정 학년도, 특정일 출석 데이터 조회")
    public ResponseEntity<List<StudentAttendanceResponse>> getClassAttendanceByDate(
        @PathVariable Long classRoomId,
        @PathVariable Integer schoolYear,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<StudentAttendanceResponse> list =
            attendanceService.findStudentAttendances(classRoomId, schoolYear, date);
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
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // date 기준 학년도를 내부에서 계산하거나, StudentClassService 레벨에서
        // 만약 학년도가 필요하면 service 메서드 시그니처를 바꿔주세요.
        List<StudentAttendanceResponse> list =
            attendanceService.findStudentAttendancesByClassAndDate(classRoomId, date);

        return ResponseEntity.ok(list);
    }

    @GetMapping("/classrooms/{classRoomId}/sundays/summary")
    @Operation(summary = "특정 반의 일요일별 출석 요약 조회")
    public ResponseEntity<List<ClassSundayAttendanceResponse>> getSundayAttendanceSummaryForClass(
        @PathVariable Long classRoomId
    ) {
        List<ClassSundayAttendanceResponse> list =
            attendanceService.getSundayAttendanceSummaryForClass(classRoomId);
        return ResponseEntity.ok(list);
    }
}

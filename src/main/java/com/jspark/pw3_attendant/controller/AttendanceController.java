package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import com.jspark.pw3_attendant.service.Attendance.AttendanceService;

import com.jspark.pw3_attendant.service.Attendance.dto.AttendanceResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.StudentAttendanceResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.UpsertAttendanceRequest;
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

    @PutMapping("/{studentClassId}/{date}")
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
    public List<AttendanceResponse> getAttendancesByStudentClass(@PathVariable Long studentClassId) {
        return attendanceService.findByStudentClass(studentClassId).stream()
            .map(AttendanceResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * 해당 반 id, 해당 학년도, 해당 일자
     * */
    @GetMapping("/year/{schoolYear}/date/{date}")
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
    public ResponseEntity<List<StudentAttendanceResponse>> getClassAttendanceByDate(
        @PathVariable Long classRoomId,
        @PathVariable Integer schoolYear,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<StudentAttendanceResponse> list =
            attendanceService.findStudentAttendances(classRoomId, schoolYear, date);
        return ResponseEntity.ok(list);
    }


}

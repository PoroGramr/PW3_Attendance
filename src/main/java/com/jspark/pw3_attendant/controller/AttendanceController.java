package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.AttendanceStatus;
import com.jspark.pw3_attendant.service.AttendanceService;
import com.jspark.pw3_attendant.service.dto.AttendanceRequest;
import com.jspark.pw3_attendant.service.dto.AttendanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * 출석 체크 저장
     */
    @PostMapping
    public void saveAttendance(@RequestBody AttendanceRequest request) {
        attendanceService.saveAttendance(
            request.getStudentId(),
            request.getClassRoomId(),
            request.getSchoolYear(),
            request.getDate(),
            AttendanceStatus.valueOf(request.getStatus())
        );
    }

    /**
     * 특정 반 + 학년도 + 날짜로 학생 출석 현황 조회
     */
    @GetMapping("/classroom/{classRoomId}")
    public List<AttendanceResponse> getAttendanceByClassRoom(@PathVariable Long classRoomId,
        @RequestParam Integer schoolYear,
        @RequestParam LocalDate date) {
        return attendanceService.findAttendancesByClassRoomAndDate(classRoomId, schoolYear, date)
            .stream()
            .map(AttendanceResponse::from)
            .collect(Collectors.toList());
    }
}

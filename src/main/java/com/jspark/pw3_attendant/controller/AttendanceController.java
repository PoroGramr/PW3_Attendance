package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.AttendanceStatus;
import com.jspark.pw3_attendant.service.AttendanceService;
import com.jspark.pw3_attendant.service.dto.AttendanceRequest;
import com.jspark.pw3_attendant.service.dto.AttendanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    public void createAttendance(@RequestBody AttendanceRequest request) {
        attendanceService.saveAttendance(
            request.getStudentClassId(),
            request.getDate(),
            AttendanceStatus.valueOf(request.getStatus())
        );
    }

    @GetMapping("/{studentClassId}")
    public List<AttendanceResponse> getAttendancesByStudentClass(@PathVariable Long studentClassId) {
        return attendanceService.findByStudentClass(studentClassId)
            .stream()
            .map(AttendanceResponse::from)
            .collect(Collectors.toList());
    }
}

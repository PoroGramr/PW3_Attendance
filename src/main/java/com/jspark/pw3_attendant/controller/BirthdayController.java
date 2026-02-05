package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.service.birthday.BirthdayService;
import com.jspark.pw3_attendant.service.birthday.dto.MonthlyBirthdayResponse;
import com.jspark.pw3_attendant.service.birthday.dto.StudentBirthdayResponse;
import com.jspark.pw3_attendant.service.birthday.dto.TeacherBirthdayResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "생일 안내 ", description = "월별 생일자 안내")
@RestController
@RequestMapping("/api/birthday")
@RequiredArgsConstructor
public class BirthdayController {

    private final BirthdayService birthdayService;

    @GetMapping("/students/{month}")
    @Operation(summary = "특정 월 학생 생일자 조회")
    public ResponseEntity<List<StudentBirthdayResponse>> getStudentBirthdays(
        @PathVariable @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(birthdayService.getStudentBirthdays(month));
    }

    @GetMapping("/teachers/{month}")
    @Operation(summary = "특정 월 선생님 생일자 조회")
    public ResponseEntity<List<TeacherBirthdayResponse>> getTeacherBirthdays(
        @PathVariable @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(birthdayService.getTeacherBirthdays(month));
    }

    @GetMapping("/{month}")
    @Operation(summary = "특정 월 전체 생일자 조회 (학생 + 선생님)")
    public ResponseEntity<MonthlyBirthdayResponse> getAllBirthdays(
        @PathVariable @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(birthdayService.getAllBirthdays(month));
    }

}

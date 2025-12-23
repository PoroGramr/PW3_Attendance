package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.service.TeacherClass.TeacherClassService;
import com.jspark.pw3_attendant.service.TeacherClass.dto.TeacherClassRequest;
import com.jspark.pw3_attendant.service.TeacherClass.dto.TeacherClassResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher-classes")
public class TeacherClassController {

    private final TeacherClassService teacherClassService;

    @PostMapping
    @Operation(summary = "선생님-반 배정 (생성/수정)")
    public ResponseEntity<TeacherClassResponse> assignTeacherToClass(@RequestBody TeacherClassRequest request) {
        TeacherClassResponse response = teacherClassService.assignTeacherToClass(request);
        return ResponseEntity.ok(response);
    }
}

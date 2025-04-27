package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.StudentClass;
import com.jspark.pw3_attendant.service.StudentClassService;
import com.jspark.pw3_attendant.service.dto.StudentClassRequest;
import com.jspark.pw3_attendant.service.dto.StudentClassResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student-classes")
public class StudentClassController {

    private final StudentClassService studentClassService;

    @PostMapping
    public StudentClassResponse createStudentClass(@RequestBody StudentClassRequest request) {
        StudentClass studentClass = studentClassService.save(request);
        return StudentClassResponse.from(studentClass);
    }
}

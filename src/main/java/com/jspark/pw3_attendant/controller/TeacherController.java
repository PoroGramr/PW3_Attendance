package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.service.Student.StudentService;
import com.jspark.pw3_attendant.service.Student.dto.StudentRequest;
import com.jspark.pw3_attendant.service.Student.dto.StudentResponse;
import com.jspark.pw3_attendant.service.Teacher.TeacherService;
import com.jspark.pw3_attendant.service.Teacher.dto.TeacherRequest;
import com.jspark.pw3_attendant.service.Teacher.dto.TeacherResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    /**
     * 선생님 등록
     */
    @PostMapping
    @Operation(summary = "선생님 생성")
    public TeacherResponse createTeacher(@RequestBody TeacherRequest request) {
        Teacher savedTeacher = teacherService.save(request);
        return TeacherResponse.from(savedTeacher);
    }

    /**
     * 선생님 단건 조회
     */
    @GetMapping("/{id}")
    @Operation(summary = "선생님 단일 조회")
    public TeacherResponse getTeacher(@PathVariable Long id) {
        Teacher teacher = teacherService.findById(id);
        return TeacherResponse.from(teacher);
    }

    /**
     * 모든 선생님 조회
     */
    @GetMapping
    @Operation(summary = "선생님 전체 조회")
    public List<TeacherResponse> getAllTeacher() {
        return teacherService.findAll()
            .stream()
            .map(TeacherResponse::from)
            .collect(Collectors.toList());
    }

}

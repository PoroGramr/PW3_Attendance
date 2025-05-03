package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.service.Student.StudentService;
import com.jspark.pw3_attendant.service.Student.dto.StudentRequest;
import com.jspark.pw3_attendant.service.Student.dto.StudentResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    /**
     * 학생 등록
     */
    @PostMapping
    @Operation(summary = "학생 생성")
    public StudentResponse createStudent(@RequestBody StudentRequest request) {
        Student savedStudent = studentService.save(request);
        return StudentResponse.from(savedStudent);
    }

    /**
     * 학생 단건 조회
     */
    @GetMapping("/{id}")
    @Operation(summary = "학생 단일 조회")
    public StudentResponse getStudent(@PathVariable Long id) {
        Student student = studentService.findById(id);
        return StudentResponse.from(student);
    }

    /**
     * 모든 학생 조회
     */
    @GetMapping
    @Operation(summary = "학생 전체 조회")
    public List<StudentResponse> getAllStudents() {
        return studentService.findAll()
            .stream()
            .map(StudentResponse::from)
            .collect(Collectors.toList());
    }

    @GetMapping("/year")
    public List<StudentResponse> listByYear(@RequestParam Integer year) {
        return studentService.getStudentsByYear(year);
    }

}


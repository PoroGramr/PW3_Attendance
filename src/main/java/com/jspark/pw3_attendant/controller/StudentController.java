package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.Student;
import com.jspark.pw3_attendant.service.StudentService;
import com.jspark.pw3_attendant.service.dto.StudentRequest;
import com.jspark.pw3_attendant.service.dto.StudentResponse;
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
    public StudentResponse createStudent(@RequestBody StudentRequest request) {
        Student student = new Student();
        student.setName(request.getName());
        Student savedStudent = studentService.save(student);
        return StudentResponse.from(savedStudent);
    }

    /**
     * 학생 단건 조회
     */
    @GetMapping("/{id}")
    public StudentResponse getStudent(@PathVariable Long id) {
        Student student = studentService.findById(id);
        return StudentResponse.from(student);
    }

    /**
     * 모든 학생 조회
     */
    @GetMapping
    public List<StudentResponse> getAllStudents() {
        return studentService.findAll()
            .stream()
            .map(StudentResponse::from)
            .collect(Collectors.toList());
    }
}


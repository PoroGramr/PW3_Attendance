package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.service.Student.StudentService;
import com.jspark.pw3_attendant.service.Student.dto.StudentRequest;
import com.jspark.pw3_attendant.service.Student.dto.StudentResponse;
import com.jspark.pw3_attendant.service.Teacher.TeacherService;
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
     * 학생 등록
     */
    @PostMapping
    public StudentResponse createStudent(@RequestBody StudentRequest request) {
        Student savedStudent = studentService.save(request);
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

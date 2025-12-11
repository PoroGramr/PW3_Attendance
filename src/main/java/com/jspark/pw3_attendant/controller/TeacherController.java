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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @Operation(summary = "선생님 생성")
    public TeacherResponse createTeacher(@RequestBody TeacherRequest request) {
        Teacher savedTeacher = teacherService.save(request);
        return TeacherResponse.from(savedTeacher);
    }

    @GetMapping("/{id}")
    @Operation(summary = "선생님 단일 조회")
    public TeacherResponse getTeacher(@PathVariable Long id) {
        Teacher teacher = teacherService.findById(id);
        return TeacherResponse.from(teacher);
    }

    @GetMapping
    @Operation(summary = "선생님 전체 조회")
    public List<TeacherResponse> getAllTeacher() {
        return teacherService.findAll()
            .stream()
            .map(TeacherResponse::from)
            .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    @Operation(summary = "선생님 수정")
    public ResponseEntity<Teacher> updateTeacher(@PathVariable Long id, @RequestBody TeacherRequest request) {
        Teacher updatedTeacher = teacherService.updateTeacher(id, request);
        return ResponseEntity.ok(updatedTeacher);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "선생님 삭제")
    public ResponseEntity<String> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteById(id);
        return ResponseEntity.ok("선생님이 삭제되었습니다.");
    }

}

package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.StudentClass;
import com.jspark.pw3_attendant.service.StudentClassService;
import com.jspark.pw3_attendant.service.dto.StudentClassRequest;
import com.jspark.pw3_attendant.service.dto.StudentClassResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student-classes")
public class StudentClassController {

    private final StudentClassService studentClassService;

    /**
     * 학생-반 매핑 등록
     */
    @PostMapping
    public StudentClassResponse createStudentClass(@RequestBody StudentClassRequest request) {
        StudentClass savedStudentClass = studentClassService.save(request);
        return StudentClassResponse.from(savedStudentClass);
    }

    /**
     * 학생-반 매핑 단건 조회
     */
    @GetMapping("/{id}")
    public StudentClassResponse getStudentClass(@PathVariable Long id) {
        StudentClass studentClass = studentClassService.findById(id);
        return StudentClassResponse.from(studentClass);
    }

    /**
     * 특정 반 + 학년도 학생 리스트 조회
     */
    @GetMapping("/classroom/{classRoomId}")
    public List<StudentClassResponse> getStudentClassesByClassRoom(@PathVariable Long classRoomId,
        @RequestParam Integer schoolYear) {
        List<StudentClass> studentClasses = studentClassService.findAllByClassRoomAndYear(classRoomId, schoolYear);
        return studentClasses.stream()
            .map(StudentClassResponse::from)
            .collect(Collectors.toList());
    }
}


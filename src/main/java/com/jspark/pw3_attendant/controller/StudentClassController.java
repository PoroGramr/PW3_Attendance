package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.StudentClass;
import com.jspark.pw3_attendant.service.StudentClassService;
import com.jspark.pw3_attendant.service.dto.ClassRoomStudentsResponse;
import com.jspark.pw3_attendant.service.dto.StudentClassRequest;
import com.jspark.pw3_attendant.service.dto.StudentClassResponse;
import com.jspark.pw3_attendant.service.dto.StudentSummaryResponse;
import java.util.List;
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

    @GetMapping("/classroom/{classRoomId}")
    public List<StudentSummaryResponse> getStudentsByClassRoomAndYear(
        @PathVariable Long classRoomId,
        @RequestParam Integer schoolYear
    ) {
        return studentClassService.findStudentsByClassRoomAndYear(classRoomId, schoolYear);
    }

    @GetMapping("/school-year/{schoolYear}")
    public List<ClassRoomStudentsResponse> getStudentsGroupedByClassRoom(@PathVariable Integer schoolYear) {
        return studentClassService.findAllStudentsGroupedByClassRoom(schoolYear);
    }
}

package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomTeacherResponse;
import com.jspark.pw3_attendant.service.Student.dto.StudentResponse;
import com.jspark.pw3_attendant.service.StudentClass.StudentClassService;
import com.jspark.pw3_attendant.service.StudentClass.dto.ClassRoomIdStudentsResponse;
import com.jspark.pw3_attendant.service.StudentClass.dto.StudentClassRequest;
import com.jspark.pw3_attendant.service.StudentClass.dto.StudentClassResponse;
import com.jspark.pw3_attendant.service.StudentClass.dto.StudentSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student-classes")
public class StudentClassController {

    private final StudentClassService studentClassService;

    @PostMapping
    @Operation(summary = "특정 학년도에 특정 학생을 반에 배정")
    public StudentClassResponse createStudentClass(@RequestBody StudentClassRequest request) {
        StudentClass studentClass = studentClassService.save(request);
        return StudentClassResponse.from(studentClass);
    }

    @GetMapping("/classroom/{classRoomId}")
    @Operation(summary = "특정 학년도에 특정 반의 학생 리스트 조회")
    public List<StudentSummaryResponse> getStudentsByClassRoomAndYear(
        @PathVariable Long classRoomId,
        @RequestParam Integer schoolYear
    ) {
        return studentClassService.findStudentsByClassRoomAndYear(classRoomId, schoolYear);
    }


    @Operation(summary = "특정 학년도에 존재하는 반 + 학생들 리스트")
    @GetMapping("/school-year/{schoolYear}")
    public List<ClassRoomIdStudentsResponse> getStudentsGroupedByClassRoom(@PathVariable Integer schoolYear) {
        return studentClassService.findAllStudentsGroupedByClassRoom(schoolYear);
    }

    @GetMapping("/class/{classRoomId}/year/{schoolYear}")
    @Operation(summary = "해당 학년도 존재하는 특정 반 학생들 리스트")
    public ResponseEntity<List<StudentResponse>> getStudentsByClassAndYear(
        @PathVariable Long classRoomId,
        @PathVariable Integer schoolYear
    ) {
        List<StudentResponse> students = studentClassService.findStudentsByClassAndYear(classRoomId, schoolYear);
        return ResponseEntity.ok(students);
    }

    @GetMapping("/year/{schoolYear}/class-rooms")
    @Operation(summary = "해당 학년도 존재하는 반 리스트")
    public ResponseEntity<List<ClassRoomTeacherResponse>> getClassRoomsByYear(
        @PathVariable Integer schoolYear
    ) {
        List<ClassRoomTeacherResponse> rooms = studentClassService.findClassRoomsByYear(schoolYear);
        return ResponseEntity.ok(rooms);
    }

}

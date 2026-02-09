package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.service.Student.StudentService;
import com.jspark.pw3_attendant.service.Student.dto.StudentRequest;
import com.jspark.pw3_attendant.service.Student.dto.StudentResponse;
import com.jspark.pw3_attendant.service.StudentClass.StudentClassService;

import com.jspark.pw3_attendant.service.qr.QrService;
import com.jspark.pw3_attendant.service.Student.dto.MonthlyStudentRegistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/students") // URL 경로 변경
public class StudentController {

    private final StudentService studentService;
    private final StudentClassService studentClassService;
    private final QrService qrService; // QrService 의존성 주입

    @PostMapping
    @Operation(summary = "학생 생성")
    public StudentResponse createStudent(@RequestBody StudentRequest request) {
        Student savedStudent = studentService.save(request);
        return StudentResponse.from(savedStudent, Map.of());
    }

    @DeleteMapping("/graduated/{id}")
    @Operation(summary = "학생 졸업")
    public ResponseEntity<String> graduatedStudent(@PathVariable Long id) {
        studentService.graduatedStudent(id);
        return ResponseEntity.ok("학생이 졸업되었습니다.");
    }

    @PostMapping("/{studentId}/send-qr")
    @Operation(summary = "학생에게 QR 코드 링크 발송")
    public ResponseEntity<String> sendQrCode(@PathVariable Long studentId) {
        boolean success = qrService.sendPersonalQrCodeSms(studentId);
        if (success) {
            return ResponseEntity.ok("QR 코드가 성공적으로 발송되었습니다.");
        } else {
            return ResponseEntity.internalServerError().body("QR 코드 발송에 실패했습니다.");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable Long id, @RequestBody StudentRequest request) {
        StudentResponse updatedStudent = studentService.updateStudent(id, request);
        return ResponseEntity.ok(updatedStudent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok("학생이 삭제되었습니다.");
    }
    // ... (기존 GET 메서드들)

    @GetMapping("/{id}")
    @Operation(summary = "학생 단일 조회")
    public StudentResponse getStudent(@PathVariable Long id) {
        try {
            return studentService.findById(id);
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다.");
        }
    }

    /**
     * 모든 학생 조회
     */
    @GetMapping
    @Operation(summary = "학생 전체 조회", description = "졸업생 제외")
    public List<StudentResponse> getAllStudents() {
        return studentService.findAllWithoutGraduated();
    }

    @GetMapping("/with-graduated")
    @Operation(summary = "졸업생 포함 학생 전체 조회")
    public List<StudentResponse> getAllStudentsWithGraduated() {
        return studentService.findAll();
    }

    @GetMapping("/studentsWithClassInfo")
    public List<StudentResponse> getStudentsWithClassInfo(@RequestParam Integer schoolYear) {
        return studentClassService.findStudentsWithClassInfo(schoolYear);
    }

    @GetMapping("/year")
    public List<StudentResponse> listByYear(@RequestParam Integer year) {
        return studentService.getStudentsByYear(year);
    }

    @GetMapping("/registrations/by-year/{year}")
    @Operation(summary = "연도별 월별 학생 등록 현황")
    public List<MonthlyStudentRegistrationResponse> getMonthlyRegistrations(@PathVariable int year) {
        return studentService.findStudentsByYearGroupByMonth(year);
    }
}

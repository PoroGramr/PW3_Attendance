package com.jspark.pw3_attendant.service.Student;


import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.service.Student.dto.MonthlyStudentRegistrationResponse;
import com.jspark.pw3_attendant.service.Student.dto.StudentInfo;
import com.jspark.pw3_attendant.service.Student.dto.StudentRequest;
import com.jspark.pw3_attendant.service.Student.dto.StudentResponse;
import java.time.LocalDate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentClassRepository studentClassRepository;

    @Transactional
    public Student save(StudentRequest request) {
        Student student = new Student();
        student.setName(request.getName());
        student.setBirth(request.getBirth());
        student.setPhone(request.getPhone());
        return studentRepository.save(student);
    }

    public Student updateStudent(Long id, Student studentDetails) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다."));

        student.setName(studentDetails.getName());
        student.setBirth(studentDetails.getBirth());
        student.setPhone(studentDetails.getPhone());

        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }

    public Student findById(Long id) {
        return studentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));
    }



    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public List<StudentResponse> getStudentsByYear(Integer year) {
        return studentClassRepository.findAllBySchoolYear(year).stream()
            .map(StudentResponse::from)
            .collect(Collectors.toList());
    }

    public List<MonthlyStudentRegistrationResponse> findStudentsByYearGroupByMonth(int year) {
        List<Student> students = studentRepository.findAllByYear(year);
        LocalDate exclusionDate = LocalDate.of(2025, 5, 10);

        Map<Integer, List<StudentInfo>> monthlyStudents = students.stream()
                .filter(student -> !student.getCreatedAt().toLocalDate().isEqual(exclusionDate))
                .collect(Collectors.groupingBy(
                        student -> student.getCreatedAt().getMonthValue(),
                        Collectors.mapping(StudentInfo::from, Collectors.toList())
                ));

        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> MonthlyStudentRegistrationResponse.builder()
                        .month(month)
                        .students(monthlyStudents.getOrDefault(month, List.of()))
                        .build())
                .collect(Collectors.toList());
    }
}

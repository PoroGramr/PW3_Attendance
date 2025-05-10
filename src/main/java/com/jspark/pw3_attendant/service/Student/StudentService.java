package com.jspark.pw3_attendant.service.Student;


import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.service.Student.dto.StudentRequest;
import com.jspark.pw3_attendant.service.Student.dto.StudentResponse;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다."));
        System.out.println("삭제 들어감");
        studentRepository.delete(student);
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
}

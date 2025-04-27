package com.jspark.pw3_attendant.service;

import com.jspark.pw3_attendant.domain.Student;
import com.jspark.pw3_attendant.repository.StudentRepository;
import com.jspark.pw3_attendant.service.dto.StudentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;

    @Transactional
    public Student save(StudentRequest request) {
        Student student = new Student();
        student.setName(request.getName());
        return studentRepository.save(student);
    }

    public Student findById(Long id) {
        return studentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));
    }

    public List<Student> findAll() {
        return studentRepository.findAll();
    }
}

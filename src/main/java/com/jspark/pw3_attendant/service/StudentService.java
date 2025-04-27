package com.jspark.pw3_attendant.service;

import com.jspark.pw3_attendant.domain.Student;
import com.jspark.pw3_attendant.repository.StudentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;

    public Student findById(Long studentId) {
        return studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));
    }

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    @Transactional
    public Student save(Student student) {
        return studentRepository.save(student);
    }

    @Transactional
    public void delete(Long studentId) {
        studentRepository.deleteById(studentId);
    }
}


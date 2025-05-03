package com.jspark.pw3_attendant.service.Student;


import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.service.Student.dto.StudentRequest;
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
        student.setBirth(request.getBirth());
        student.setPhone(request.getPhone());
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

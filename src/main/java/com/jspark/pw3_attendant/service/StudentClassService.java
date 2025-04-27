package com.jspark.pw3_attendant.service;

import com.jspark.pw3_attendant.domain.ClassRoom;
import com.jspark.pw3_attendant.domain.Student;
import com.jspark.pw3_attendant.domain.StudentClass;
import com.jspark.pw3_attendant.repository.ClassRoomRepository;
import com.jspark.pw3_attendant.repository.StudentClassRepository;
import com.jspark.pw3_attendant.repository.StudentRepository;
import com.jspark.pw3_attendant.service.dto.StudentClassRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentClassService {

    private final StudentClassRepository studentClassRepository;
    private final StudentRepository studentRepository;
    private final ClassRoomRepository classRoomRepository;

    @Transactional
    public StudentClass save(StudentClassRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
            .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));
        ClassRoom classRoom = classRoomRepository.findById(request.getClassRoomId())
            .orElseThrow(() -> new IllegalArgumentException("반을 찾을 수 없습니다."));

        StudentClass studentClass = new StudentClass();
        studentClass.setStudent(student);
        studentClass.setClassRoom(classRoom);
        studentClass.setSchoolYear(request.getSchoolYear());

        return studentClassRepository.save(studentClass);
    }

    public StudentClass findById(Long id) {
        return studentClassRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("학생-반 매핑을 찾을 수 없습니다."));
    }

    public List<StudentClass> findAllByClassRoomAndYear(Long classRoomId, Integer schoolYear) {
        return studentClassRepository.findAllByClassRoomIdAndSchoolYear(classRoomId, schoolYear);
    }
}

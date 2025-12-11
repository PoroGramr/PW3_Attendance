package com.jspark.pw3_attendant.service.Teacher;


import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.repository.Teacher.TeacherRepository;
import com.jspark.pw3_attendant.service.Teacher.dto.TeacherRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public Teacher findById(Long teacherId) {
        return teacherRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("선생님을 찾을 수 없습니다."));
    }

    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    @Transactional
    public Teacher save(TeacherRequest request) {
        Teacher teacher = new Teacher();
        teacher.setName(request.getName());
        teacher.setBirth(request.getBirth());
        teacher.setPhone(request.getPhone());
        teacher.setSex(request.getSex());
        teacher.setTeacherType(request.getTeacherType());
        teacher.setMemo(request.getMemo());
        return teacherRepository.save(teacher);
    }

    @Transactional
    public Teacher updateTeacher(Long id, TeacherRequest request) {
        Teacher teacher = teacherRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("선생님을 찾을 수 없습니다."));

        teacher.setName(request.getName());
        teacher.setBirth(request.getBirth());
        teacher.setPhone(request.getPhone());
        teacher.setSex(request.getSex());
        teacher.setTeacherType(request.getTeacherType());
        teacher.setMemo(request.getMemo());

        return teacherRepository.save(teacher);
    }

    @Transactional
    public void deleteById(Long id) {
        teacherRepository.deleteById(id);
    }
}

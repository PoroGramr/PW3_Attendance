package com.jspark.pw3_attendant.service.TeacherClass;


import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.domain.TeacherClass.TeacherClass;
import com.jspark.pw3_attendant.repository.ClassRoom.ClassRoomRepository;
import com.jspark.pw3_attendant.repository.Teacher.TeacherRepository;
import com.jspark.pw3_attendant.repository.TeacherClass.TeacherClassRepository;
import com.jspark.pw3_attendant.service.TeacherClass.dto.TeacherClassRequest;
import com.jspark.pw3_attendant.service.TeacherClass.dto.TeacherClassResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherClassService {

    private final TeacherClassRepository teacherClassRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRoomRepository classRoomRepository;

    public TeacherClass findTeacherByClassRoomAndYear(Long classRoomId, Integer schoolYear) {
        return teacherClassRepository.findByClassRoomIdAndSchoolYear(classRoomId, schoolYear)
            .orElseThrow(() -> new IllegalArgumentException("해당 반의 담당 선생님을 찾을 수 없습니다."));
    }

    @Transactional
    public TeacherClassResponse assignTeacherToClass(TeacherClassRequest request) {
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("해당 선생님을 찾을 수 없습니다."));
        ClassRoom classRoom = classRoomRepository.findById(request.getClassRoomId())
                .orElseThrow(() -> new IllegalArgumentException("해당 반을 찾을 수 없습니다."));

        Optional<TeacherClass> existingMapping = teacherClassRepository.findByClassRoomIdAndSchoolYear(request.getClassRoomId(), request.getSchoolYear());

        TeacherClass teacherClass;
        if (existingMapping.isPresent()) {
            // Update existing mapping
            teacherClass = existingMapping.get();
            teacherClass.setTeacher(teacher);
        } else {
            // Create new mapping
            teacherClass = new TeacherClass();
            teacherClass.setTeacher(teacher);
            teacherClass.setClassRoom(classRoom);
            teacherClass.setSchoolYear(request.getSchoolYear());
            teacherClassRepository.save(teacherClass);
        }

        return TeacherClassResponse.from(teacherClass);
    }
}


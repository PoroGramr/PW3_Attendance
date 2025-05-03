package com.jspark.pw3_attendant.service.TeacherClass;


import com.jspark.pw3_attendant.domain.TeacherClass.TeacherClass;
import com.jspark.pw3_attendant.repository.TeacherClass.TeacherClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherClassService {

    private final TeacherClassRepository teacherClassRepository;

    public TeacherClass findTeacherByClassRoomAndYear(Long classRoomId, Integer schoolYear) {
        return teacherClassRepository.findByClassRoomIdAndSchoolYear(classRoomId, schoolYear)
            .orElseThrow(() -> new IllegalArgumentException("해당 반의 담당 선생님을 찾을 수 없습니다."));
    }
}


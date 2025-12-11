package com.jspark.pw3_attendant.service.Teacher;


import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.domain.TeacherClass.TeacherClass;
import com.jspark.pw3_attendant.repository.Teacher.TeacherRepository;
import com.jspark.pw3_attendant.repository.TeacherClass.TeacherClassRepository;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomResponse;
import com.jspark.pw3_attendant.service.Teacher.dto.TeacherRequest;
import com.jspark.pw3_attendant.service.Teacher.dto.TeacherResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;

    public TeacherResponse findById(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("선생님을 찾을 수 없습니다."));

        Map<Integer, List<ClassRoomResponse>> classesByYear = teacherClassRepository.findAllByTeacher(teacher).stream()
            .collect(Collectors.groupingBy(
                TeacherClass::getSchoolYear,
                Collectors.mapping(tc -> ClassRoomResponse.from(tc.getClassRoom()), Collectors.toList())
            ));

        return TeacherResponse.from(teacher, classesByYear);
    }

    public List<TeacherResponse> findAll() {
        return teacherRepository.findAll().stream()
            .map(teacher -> {
                Map<Integer, List<ClassRoomResponse>> classesByYear = teacherClassRepository.findAllByTeacher(teacher).stream()
                    .collect(Collectors.groupingBy(
                        TeacherClass::getSchoolYear,
                        Collectors.mapping(tc -> ClassRoomResponse.from(tc.getClassRoom()), Collectors.toList())
                    ));
                return TeacherResponse.from(teacher, classesByYear);
            })
            .collect(Collectors.toList());
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

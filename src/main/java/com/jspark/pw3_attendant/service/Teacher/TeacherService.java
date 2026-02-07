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
        Teacher teacher = teacherRepository.findByIdAndDeletedAtIsNull(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("선생님을 찾을 수 없습니다."));

        Map<Integer, List<ClassRoomResponse>> classesByYear = teacherClassRepository.findAllByTeacher(teacher).stream()
                .collect(Collectors.groupingBy(
                        TeacherClass::getSchoolYear,
                        Collectors.mapping(tc -> ClassRoomResponse.from(tc.getClassRoom()), Collectors.toList())));

        return TeacherResponse.from(teacher, classesByYear);
    }

    public List<TeacherResponse> findAll() {
        // 1. 모든 선생님 조회
        List<Teacher> teachers = teacherRepository.findAllByDeletedAtIsNull();

        if (teachers.isEmpty()) {
            return List.of();
        }

        // 2. 모든 TeacherClass를 한 번에 조회 (N+1 방지, ClassRoom, Teacher 함께 로드)
        List<TeacherClass> allTeacherClasses = teacherClassRepository.findAllWithTeacherAndClassRoom();

        // 3. teacherId로 그룹핑
        Map<Long, Map<Integer, List<ClassRoomResponse>>> teacherClassMap = allTeacherClasses.stream()
                .collect(Collectors.groupingBy(
                        tc -> tc.getTeacher().getId(),
                        Collectors.groupingBy(
                                TeacherClass::getSchoolYear,
                                Collectors.mapping(tc -> ClassRoomResponse.from(tc.getClassRoom()),
                                        Collectors.toList()))));

        // 4. 선생님별로 반 정보 매핑
        return teachers.stream()
                .map(teacher -> {
                    Map<Integer, List<ClassRoomResponse>> classesByYear = teacherClassMap
                            .getOrDefault(teacher.getId(), Map.of());
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
        Teacher teacher = teacherRepository.findByIdAndDeletedAtIsNull(id)
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

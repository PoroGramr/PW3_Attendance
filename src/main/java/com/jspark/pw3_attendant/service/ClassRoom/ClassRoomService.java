package com.jspark.pw3_attendant.service.ClassRoom;


import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import com.jspark.pw3_attendant.domain.TeacherClass.TeacherClass;
import com.jspark.pw3_attendant.repository.ClassRoom.ClassRoomRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.repository.TeacherClass.TeacherClassRepository;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomDetailResponse;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomRequest;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomResponse;
import com.jspark.pw3_attendant.service.StudentClass.dto.StudentSummaryResponse;
import com.jspark.pw3_attendant.service.Teacher.dto.TeacherResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassRoomService {

    private final ClassRoomRepository classRoomRepository;
    private final StudentClassRepository studentClassRepository;
    private final TeacherClassRepository teacherClassRepository;

    public ClassRoom findById(Long classRoomId) {
        return classRoomRepository.findById(classRoomId)
            .orElseThrow(() -> new IllegalArgumentException("반을 찾을 수 없습니다."));
    }

    public List<ClassRoom> findAll() {
        return classRoomRepository.findAll();
    }

    @Transactional
    public ClassRoom save(ClassRoomRequest request) {
        ClassRoom classRoom = new ClassRoom();
        classRoom.setSchoolType(request.getSchoolType());
        classRoom.setGrade(request.getGrade());
        classRoom.setClassNumber(request.getClassNumber());
        return classRoomRepository.save(classRoom);
    }
    @Transactional
    public void delete(Long classRoomId) {
        classRoomRepository.deleteById(classRoomId);
    }

    public ClassRoomDetailResponse getClassRoomDetails(int schoolYear, ClassRoom.SchoolType schoolType, int grade, int classNumber) {
        // 1. ClassRoom 정보 조회 (schoolYear 없이 조회)
        ClassRoom classRoom = classRoomRepository.findBySchoolTypeAndGradeAndClassNumber(schoolType, grade, classNumber)
            .orElseThrow(() -> new IllegalArgumentException("해당하는 반 정보를 찾을 수 없습니다."));

        // 2. 학생 목록 조회 및 변환 (classRoomId와 schoolYear 사용)
        List<StudentClass> studentClasses = studentClassRepository.findAllByClassRoomIdAndSchoolYear(classRoom.getId(), schoolYear);
        List<StudentSummaryResponse> studentResponses = studentClasses.stream()
            .map(studentClass -> StudentSummaryResponse.from(studentClass.getStudent()))
            .collect(Collectors.toList());

        // 3. 교사 목록 조회 및 변환 (classRoomId와 schoolYear 사용)
        List<TeacherClass> teacherClasses = teacherClassRepository.findAllByClassRoomIdAndSchoolYear(classRoom.getId(), schoolYear);
        List<TeacherResponse> teacherResponses = teacherClasses.stream()
            .map(teacherClass -> TeacherResponse.from(teacherClass.getTeacher()))
            .collect(Collectors.toList());

        // 4. 최종 응답 DTO 조합
        return new ClassRoomDetailResponse(
            ClassRoomResponse.from(classRoom),
            studentResponses,
            teacherResponses
        );
    }
}

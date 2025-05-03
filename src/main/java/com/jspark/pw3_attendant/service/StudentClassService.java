package com.jspark.pw3_attendant.service;

import com.jspark.pw3_attendant.domain.ClassRoom;
import com.jspark.pw3_attendant.domain.Student;
import com.jspark.pw3_attendant.domain.StudentClass;
import com.jspark.pw3_attendant.repository.ClassRoomRepository;
import com.jspark.pw3_attendant.repository.StudentClassRepository;
import com.jspark.pw3_attendant.repository.StudentRepository;
import com.jspark.pw3_attendant.service.dto.ClassRoomStudentsResponse;
import com.jspark.pw3_attendant.service.dto.StudentClassRequest;
import com.jspark.pw3_attendant.service.dto.StudentSummaryResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<StudentSummaryResponse> findStudentsByClassRoomAndYear(Long classRoomId, Integer schoolYear) {
        List<StudentClass> studentClasses = studentClassRepository.findAllByClassRoomIdAndSchoolYear(classRoomId, schoolYear);

        return studentClasses.stream()
            .map(sc -> StudentSummaryResponse.from(sc.getStudent()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClassRoomStudentsResponse> findAllStudentsGroupedByClassRoom(Integer schoolYear) {
        List<StudentClass> studentClasses = studentClassRepository.findAllBySchoolYear(schoolYear);

        Map<ClassRoom, List<StudentClass>> grouped = studentClasses.stream()
            .collect(Collectors.groupingBy(StudentClass::getClassRoom));

        return grouped.entrySet().stream()
            .map(entry -> {
                ClassRoom classRoom = entry.getKey();
                List<StudentSummaryResponse> students = entry.getValue().stream()
                    .map(sc -> new StudentSummaryResponse(
                        sc.getStudent().getId(),
                        sc.getStudent().getName()
                    ))
                    .collect(Collectors.toList());

                return new ClassRoomStudentsResponse(
                    classRoom.getId(),
                    classRoom.getSchoolType().name(),
                    classRoom.getGrade(),
                    classRoom.getClassNumber(),
                    students
                );
            })
            .sorted(Comparator.comparing(ClassRoomStudentsResponse::getGrade)
                .thenComparing(ClassRoomStudentsResponse::getClassNumber))
            .collect(Collectors.toList());
    }

}

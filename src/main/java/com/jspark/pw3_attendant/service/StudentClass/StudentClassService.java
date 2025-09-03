package com.jspark.pw3_attendant.service.StudentClass;


import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.domain.TeacherClass.TeacherClass;
import com.jspark.pw3_attendant.repository.ClassRoom.ClassRoomRepository;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.repository.TeacherClass.TeacherClassRepository;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomTeacherResponse;
import com.jspark.pw3_attendant.service.Student.dto.StudentResponse;
import com.jspark.pw3_attendant.service.StudentClass.dto.ClassRoomIdStudentsResponse;
import com.jspark.pw3_attendant.service.StudentClass.dto.StudentClassRequest;
import com.jspark.pw3_attendant.service.StudentClass.dto.StudentClassSummaryResponse;
import com.jspark.pw3_attendant.service.StudentClass.dto.StudentSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentClassService {

    private final StudentClassRepository studentClassRepository;
    private final StudentRepository studentRepository;
    private final ClassRoomRepository classRoomRepository;
    private final TeacherClassRepository teacherClassRepository;

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
    public List<ClassRoomIdStudentsResponse> findAllStudentsGroupedByClassRoom(Integer schoolYear) {
        // 1) year 에 해당하는 매핑 전부 로드
        List<StudentClass> studentClasses = studentClassRepository.findAllBySchoolYear(schoolYear);

        // 2) 반별로 그룹핑
        Map<ClassRoom, List<StudentClass>> byRoom =
            studentClasses.stream()
                .collect(Collectors.groupingBy(StudentClass::getClassRoom));

        // 3) DTO 변환
        return byRoom.entrySet().stream()
            .map(entry -> {
                ClassRoom room = entry.getKey();
                List<StudentClassSummaryResponse> students = entry.getValue().stream()
                    .map(StudentClassSummaryResponse::from)
                    .collect(Collectors.toList());

                // Find teacher for the classroom and year
                String teacherName = teacherClassRepository.findByClassRoomIdAndSchoolYear(room.getId(), schoolYear)
                    .map(teacherClass -> teacherClass.getTeacher().getName())
                    .orElse(null);

                return new ClassRoomIdStudentsResponse(
                    room.getId(),
                    room.getSchoolType().name(),
                    room.getGrade(),
                    room.getClassNumber(),
                    teacherName,
                    students
                );
            })
            // 반 정렬 (학년→반 번호)
            .sorted(Comparator
                .comparing(ClassRoomIdStudentsResponse::getGrade)
                .thenComparing(ClassRoomIdStudentsResponse::getClassNumber))
            .collect(Collectors.toList());
    }

    public List<StudentResponse> findStudentsByClassAndYear(Long classRoomId, Integer schoolYear) {
        return studentClassRepository.findAllByClassRoom_IdAndSchoolYear(classRoomId, schoolYear)
            .stream()
            .map(StudentResponse::from)
            .collect(Collectors.toList());
    }

    /** 해당 학년도에 개설된 반들을 중복 제거 후 DTO로 반환 */
    public List<ClassRoomTeacherResponse> findClassRoomsByYear(Integer schoolYear) {
        List<ClassRoom> classRoomsFromStudents = studentClassRepository.findAllBySchoolYear(schoolYear).stream()
            .map(StudentClass::getClassRoom)
            .collect(Collectors.toList());

        List<ClassRoom> classRoomsFromTeachers = teacherClassRepository.findAllBySchoolYear(schoolYear).stream()
            .map(TeacherClass::getClassRoom)
            .collect(Collectors.toList());

        List<ClassRoom> classRooms = Stream.concat(classRoomsFromStudents.stream(), classRoomsFromTeachers.stream())
            .distinct()
            .collect(Collectors.toList());

        return classRooms.stream()
            .map(classRoom -> {
                Optional<TeacherClass> teacherClassOpt = teacherClassRepository.findByClassRoomIdAndSchoolYear(classRoom.getId(), schoolYear);
                Teacher teacher = teacherClassOpt.map(TeacherClass::getTeacher).orElse(null);
                return new ClassRoomTeacherResponse(classRoom, teacher);
            })
            .collect(Collectors.toList());
    }


    public List<StudentResponse> findStudentsWithClassInfo(Integer schoolYear) {
        // 1) schoolYear에 해당하는 StudentClass 목록을 가져옵니다.
        List<StudentClass> studentClasses = studentClassRepository.findAllBySchoolYear(schoolYear);

        // 2) 학생 중 StudentClass가 없거나 학년 정보가 없는 학생들을 처리하기 위해
        List<Student> studentsWithoutClass = studentRepository.findAllByStudentClassesIsEmpty();

        // 3) 학생 정보와 반 정보를 포함하여 변환
        List<StudentResponse> studentResponses = studentClasses.stream()
            .map(studentClass -> {
                Student student = studentClass.getStudent();
                return new StudentResponse(
                    student.getId(),
                    student.getName(),
                    student.getBirth(),
                    student.getPhone(),
                    studentClass.getSchoolYear(),  // schoolYear가 있을 때만 설정
                    studentClass.getClassRoom().getId()
                );
            })
            .collect(Collectors.toList());

        // 4) 학년 정보가 없는 학생들도 포함
        studentResponses.addAll(studentsWithoutClass.stream()
            .map(student -> new StudentResponse(
                student.getId(),
                student.getName(),
                student.getBirth(),
                student.getPhone(),
                null,  // schoolYear가 없는 학생은 null로 처리
                null   // 반 정보도 없으므로 null
            ))
            .collect(Collectors.toList()));

        return studentResponses;
    }


}

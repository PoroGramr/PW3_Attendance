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
import com.jspark.pw3_attendant.service.Student.StudentService;
import com.jspark.pw3_attendant.service.Student.dto.StudentResponse;
import com.jspark.pw3_attendant.service.StudentClass.dto.ClassRoomIdStudentsResponse;
import com.jspark.pw3_attendant.service.StudentClass.dto.StudentClassRequest;
import com.jspark.pw3_attendant.service.StudentClass.dto.StudentClassSummaryResponse;
import com.jspark.pw3_attendant.service.StudentClass.dto.StudentSummaryResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    private final TeacherClassRepository teacherClassRepository;
    private final StudentService studentService;

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
                    .map(TeacherClass::getTeacher)
                    .filter(teacher -> teacher.getDeletedAt() == null) // soft-deleted teacher check
                    .map(Teacher::getName)
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
        // TODO: This method causes N+1 queries. Consider optimizing if performance becomes an issue.
        return studentClassRepository.findAllByClassRoom_IdAndSchoolYear(classRoomId, schoolYear)
            .stream()
            .map(sc -> studentService.findById(sc.getStudent().getId()))
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
                Teacher teacher = teacherClassOpt.map(TeacherClass::getTeacher)
                    .filter(t -> t.getDeletedAt() == null)
                    .orElse(null);
                return new ClassRoomTeacherResponse(classRoom, teacher);
            })
            .collect(Collectors.toList());
    }


    public List<StudentResponse> findStudentsWithClassInfo(Integer schoolYear) {
        // TODO: This method causes N+1 queries. Consider optimizing if performance becomes an issue.
        return studentClassRepository.findAllBySchoolYear(schoolYear).stream()
            .map(studentClass -> studentService.findById(studentClass.getStudent().getId()))
            .collect(Collectors.toList());
    }


}

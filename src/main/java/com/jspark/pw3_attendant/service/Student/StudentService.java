package com.jspark.pw3_attendant.service.Student;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomResponse;
import com.jspark.pw3_attendant.service.Student.dto.MonthlyStudentRegistrationResponse;
import com.jspark.pw3_attendant.service.Student.dto.StudentInfo;
import com.jspark.pw3_attendant.service.Student.dto.StudentRequest;
import com.jspark.pw3_attendant.service.Student.dto.StudentResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

        private final StudentRepository studentRepository;
        private final StudentClassRepository studentClassRepository;

        @Transactional
        public Student save(StudentRequest request) {
                Student student = new Student();
                student.setName(request.getName());
                student.setBirth(request.getBirth());
                student.setPhone(request.getPhone());
                student.setParentPhone(request.getParentPhone());
                student.setSex(request.getSex());
                student.setSchool(request.getSchool());
                student.setMemo(request.getMemo());
                return studentRepository.save(student);

        }

        @Transactional
        public StudentResponse updateStudent(Long id, StudentRequest request) {
                Student student = studentRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다."));

                student.setName(request.getName());
                student.setBirth(request.getBirth());
                student.setPhone(request.getPhone());
                student.setParentPhone(request.getParentPhone());
                student.setSex(request.getSex());
                student.setSchool(request.getSchool());
                student.setMemo(request.getMemo());

                Student updatedStudent = studentRepository.save(student);

                Map<Integer, List<ClassRoomResponse>> classesByYear = studentClassRepository
                                .findAllByStudent(updatedStudent)
                                .stream()
                                .collect(Collectors.groupingBy(
                                                StudentClass::getSchoolYear,
                                                Collectors.mapping(sc -> ClassRoomResponse.from(sc.getClassRoom()),
                                                                Collectors.toList())));

                return StudentResponse.from(updatedStudent, classesByYear);
        }

        @Transactional
        public void deleteStudent(Long id) {
                Student student = studentRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));
                student.setDeletedAt(LocalDateTime.now());
                studentRepository.save(student);
        }

        public StudentResponse findById(Long id) {
                Student student = studentRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));

                Map<Integer, List<ClassRoomResponse>> classesByYear = studentClassRepository.findAllByStudent(student)
                                .stream()
                                .collect(Collectors.groupingBy(
                                                StudentClass::getSchoolYear,
                                                Collectors.mapping(sc -> ClassRoomResponse.from(sc.getClassRoom()),
                                                                Collectors.toList())));

                return StudentResponse.from(student, classesByYear);
        }

        public List<StudentResponse> findAll() {
                // 1. 모든 학생 조회
                List<Student> students = studentRepository.findAll();

                if (students.isEmpty()) {
                        return List.of();
                }

                // 2. 모든 StudentClass를 한 번에 조회 (N+1 방지, ClassRoom도 함께 로드)
                List<StudentClass> allStudentClasses = studentClassRepository.findAllWithClassRoom();

                // 3. studentId로 그룹핑
                Map<Long, Map<Integer, List<ClassRoomResponse>>> studentClassMap = allStudentClasses.stream()
                                .collect(Collectors.groupingBy(
                                                sc -> sc.getStudent().getId(),
                                                Collectors.groupingBy(
                                                                StudentClass::getSchoolYear,
                                                                Collectors.mapping(
                                                                                sc -> ClassRoomResponse.from(
                                                                                                sc.getClassRoom()),
                                                                                Collectors.toList()))));

                // 4. 학생별로 반 정보 매핑
                return students.stream()
                                .map(student -> {
                                        Map<Integer, List<ClassRoomResponse>> classesByYear = studentClassMap
                                                        .getOrDefault(student.getId(),
                                                                        Map.of());
                                        return StudentResponse.from(student, classesByYear);
                                })
                                .collect(Collectors.toList());
        }

        public List<StudentResponse> findAllWithoutGraduated() {
                // 1. 모든 학생 조회
                List<Student> students = studentRepository.findAllByIsGraduatedFalse();

                if (students.isEmpty()) {
                        return List.of();
                }

                // 2. 모든 StudentClass를 한 번에 조회 (N+1 방지, ClassRoom도 함께 로드)
                List<StudentClass> allStudentClasses = studentClassRepository.findAllWithClassRoom();

                // 3. studentId로 그룹핑
                Map<Long, Map<Integer, List<ClassRoomResponse>>> studentClassMap = allStudentClasses.stream()
                                .collect(Collectors.groupingBy(
                                                sc -> sc.getStudent().getId(),
                                                Collectors.groupingBy(
                                                                StudentClass::getSchoolYear,
                                                                Collectors.mapping(
                                                                                sc -> ClassRoomResponse.from(
                                                                                                sc.getClassRoom()),
                                                                                Collectors.toList()))));

                // 4. 학생별로 반 정보 매핑
                return students.stream()
                                .map(student -> {
                                        Map<Integer, List<ClassRoomResponse>> classesByYear = studentClassMap
                                                        .getOrDefault(student.getId(),
                                                                        Map.of());
                                        return StudentResponse.from(student, classesByYear);
                                })
                                .collect(Collectors.toList());
        }

        public List<StudentResponse> getStudentsByYear(Integer year) {
                return studentClassRepository.findAllBySchoolYear(year).stream()
                                .map(StudentClass::getStudent)
                                .distinct()
                                .map(student -> this.findById(student.getId()))
                                .collect(Collectors.toList());
        }

        public List<MonthlyStudentRegistrationResponse> findStudentsByYearGroupByMonth(int year) {
                List<Student> students = studentRepository.findAllByYear(year);
                LocalDate exclusionDate = LocalDate.of(2025, 5, 10);

                Map<Integer, List<StudentInfo>> monthlyStudents = students.stream()
                                .filter(student -> !student.getCreatedAt().toLocalDate().isEqual(exclusionDate))
                                .collect(Collectors.groupingBy(
                                                student -> student.getCreatedAt().getMonthValue(),
                                                Collectors.mapping(StudentInfo::from, Collectors.toList())));

                return IntStream.rangeClosed(1, 12)
                                .mapToObj(month -> MonthlyStudentRegistrationResponse.builder()
                                                .month(month)
                                                .students(monthlyStudents.getOrDefault(month, List.of()))
                                                .build())
                                .collect(Collectors.toList());
        }

        @Transactional
        public void graduatedStudent(Long id) {
                Student student = studentRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));
                student.setIsGraduated(true);
                studentRepository.save(student);
        }
}

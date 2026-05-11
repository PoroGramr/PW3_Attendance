package com.jspark.pw3_attendant.service.Attendance;

import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import com.jspark.pw3_attendant.domain.Attendance.AttendanceTeacher;
import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;

import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.domain.TeacherClass.TeacherClass;
import com.jspark.pw3_attendant.domain.student_qr.StudentQr;
import com.jspark.pw3_attendant.repository.Attendance.AttendanceRepository;
import com.jspark.pw3_attendant.repository.Attendance.AttendanceTeacherRepository;
import com.jspark.pw3_attendant.repository.ClassRoom.ClassRoomRepository;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.repository.Teacher.TeacherRepository;
import com.jspark.pw3_attendant.repository.TeacherClass.TeacherClassRepository;
import com.jspark.pw3_attendant.repository.student_qr.StudentQrRepository;
import com.jspark.pw3_attendant.service.Attendance.dto.*;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

        private final AttendanceRepository attendanceRepository;
        private final StudentClassRepository studentClassRepository;
        private final StudentRepository studentRepository;
        private final TeacherClassRepository teacherClassRepository;
        private final StudentQrRepository studentQrRepository;
        private final ClassRoomRepository classRoomRepository;
        private final TeacherRepository teacherRepository;
        private final AttendanceTeacherRepository attendanceTeacherRepository;
        private final SimpMessagingTemplate messagingTemplate;

        public String getDailyAttendanceReport(LocalDate date) {
                // 1. 총 출석 학생 수 계산
                List<Attendance.AttendanceStatus> studentStatuses = Arrays.asList(Attendance.AttendanceStatus.ATTEND,
                                Attendance.AttendanceStatus.LATE);
                long totalAttendedStudents = attendanceRepository.countByDateAndStatusIn(date, studentStatuses);

                // 2. 총 출석 교사 수 계산
                List<AttendanceTeacher.AttendanceStatus> teacherStatuses = Arrays
                                .asList(AttendanceTeacher.AttendanceStatus.ATTEND,
                                                AttendanceTeacher.AttendanceStatus.LATE);
                long totalAttendedTeachers = attendanceTeacherRepository.countByDateAndStatusIn(date, teacherStatuses);

                // 3. 출석한 학생 목록을 반별로 그룹화
                List<Attendance> attendedList = attendanceRepository.findByDateAndStatusIn(date, studentStatuses);
                Map<ClassRoom, List<Student>> studentsByClass = attendedList.stream()
                                .collect(Collectors.groupingBy(
                                                att -> att.getStudentClass().getClassRoom(),
                                                TreeMap::new, // 반 이름으로 정렬하기 위해 TreeMap 사용
                                                Collectors.mapping(att -> att.getStudentClass().getStudent(),
                                                                Collectors.toList())));

                // 4. 최종 리포트 문자열 생성
                StringBuilder report = new StringBuilder();
                report.append(date.toString().replace("-", ".")).append("\n");
                report.append("학생: ").append(totalAttendedStudents).append("명\n");
                report.append("선생님 (헬퍼포함): ").append(totalAttendedTeachers).append("명\n");

                studentsByClass.forEach((classRoom, students) -> {
                        // 학생 이름을 가나다순으로 정렬
                        String studentNames = students.stream()
                                        .map(Student::getName)
                                        .sorted()
                                        .collect(Collectors.joining(", "));

                        report.append("\n").append(classRoom.getName()).append(": ").append(studentNames);
                });

                return report.toString();
        }

        public DailyAttendanceSummaryResponse getDailyAttendanceSummary(LocalDate date, int schoolYear) {
                // 1. 반별 개인 출석 정보 계산 (N+1 최적화)
                List<ClassRoom> allClassRooms = classRoomRepository.findAll();
                List<StudentClass> allStudentClassesInYear = studentClassRepository.findAllBySchoolYear(schoolYear);
                List<Attendance> allAttendancesInDate = attendanceRepository
                                .findAllByStudentClassInAndDate(allStudentClassesInYear, date);

                Map<Long, List<StudentClass>> studentClassesByRoomId = allStudentClassesInYear.stream()
                                .collect(Collectors.groupingBy(sc -> sc.getClassRoom().getId()));

                Map<Long, Attendance> attendanceByStudentClassId = allAttendancesInDate.stream()
                                .collect(Collectors.toMap(att -> att.getStudentClass().getId(), att -> att));

                List<ClassDetailedAttendanceResponse> classAttendances = allClassRooms.stream()
                                .map(classRoom -> {
                                        List<StudentClass> studentClasses = studentClassesByRoomId.getOrDefault(
                                                        classRoom.getId(),
                                                        Collections.emptyList());

                                        List<StudentAttendanceDetail> studentDetails = studentClasses.stream()
                                                        .map(sc -> {
                                                                Attendance attendance = attendanceByStudentClassId
                                                                                .get(sc.getId());
                                                                return StudentAttendanceDetail.builder()
                                                                                .studentClassId(sc.getId())
                                                                                .studentId(sc.getStudent().getId())
                                                                                .studentName(sc.getStudent().getName())
                                                                                .status(Optional.ofNullable(attendance)
                                                                                                .map(Attendance::getStatus)
                                                                                                .orElse(null))
                                                                                .updatedAt(Optional
                                                                                                .ofNullable(attendance)
                                                                                                .map(Attendance::getUpdatedAt)
                                                                                                .orElse(null))
                                                                                .build();
                                                        })
                                                        .sorted(Comparator.comparing(
                                                                        StudentAttendanceDetail::getStudentName))
                                                        .collect(Collectors.toList());

                                        return new ClassDetailedAttendanceResponse(classRoom.getId(),
                                                        classRoom.getName(), studentDetails);
                                })
                                .sorted(Comparator.comparing(ClassDetailedAttendanceResponse::getClassRoomName))
                                .collect(Collectors.toList());

                // 2. 교사별 출석 정보 계산 (N+1 최적화)
                List<Teacher> allTeachers = teacherRepository.findAllByDeletedAtIsNull();
                List<AttendanceTeacher> allTeacherAttendancesInDate = attendanceTeacherRepository.findByDate(date);
                Map<Long, AttendanceTeacher> teacherAttendanceByTeacherId = allTeacherAttendancesInDate.stream()
                                .collect(Collectors.toMap(att -> att.getTeacher().getId(), att -> att));

                List<TeacherAttendanceSummary> teacherAttendances = allTeachers.stream()
                                .map(teacher -> {
                                        AttendanceTeacher attendance = teacherAttendanceByTeacherId
                                                        .get(teacher.getId());
                                        return TeacherAttendanceSummary.builder()
                                                        .teacherId(teacher.getId())
                                                        .teacherName(teacher.getName())
                                                        .status(Optional.ofNullable(attendance)
                                                                        .map(att -> AttendanceStatus.valueOf(
                                                                                        att.getStatus().name()))
                                                                        .orElse(null))
                                                        .updatedAt(
                                                                        Optional.ofNullable(attendance).map(
                                                                                        AttendanceTeacher::getUpdatedAt)
                                                                                        .orElse(null))
                                                        .build();
                                })
                                .sorted(Comparator.comparing(TeacherAttendanceSummary::getTeacherName))
                                .collect(Collectors.toList());

                // 3. 최종 응답 조합
                return new DailyAttendanceSummaryResponse(date, schoolYear, classAttendances, teacherAttendances);
        }

        @Transactional
        public ScanResponseDto processScan(
                        ScanRequestDto request) {
                String[] parts = request.getQrPayload().split(":");
                if (parts.length != 3 || !"ATT-STU".equals(parts[0])) {
                        return new ScanResponseDto("INVALID_QR");
                }

                Long studentId;
                String qrSecret;
                try {
                        studentId = Long.parseLong(parts[1]);
                        qrSecret = parts[2];
                } catch (NumberFormatException e) {
                        return new ScanResponseDto("INVALID_QR_PAYLOAD");
                }

                // 2. Validate QR Secret
                StudentQr studentQr = studentQrRepository.findByStudentId(studentId)
                                .orElseThrow(() -> new IllegalArgumentException("학생 QR 정보를 찾을 수 없습니다."));

                if (!studentQr.getQrSecret().equals(qrSecret)) {
                        return new ScanResponseDto("INVALID_QR_SECRET");
                }

                // 3. Find a valid StudentClass for the student for the current year
                int schoolYear = java.time.LocalDate.now().getYear(); // TODO: Refine school year logic
                StudentClass studentClass = studentClassRepository.findByStudentIdAndSchoolYear(studentId, schoolYear)
                                .orElseThrow(() -> new IllegalArgumentException("해당 학생은 금년에 등록된 반이 없습니다."));

                // 4. Record attendance
                // TODO: Add logic for attendance time validation (e.g., only within class
                // hours).
                boolean created = upsertAttendance(studentClass.getId(), LocalDate.now(), request.getStatus());
                Attendance attendance = attendanceRepository
                                .findByStudentClassIdAndDate(studentClass.getId(), LocalDate.now())
                                .orElseThrow(() -> new IllegalStateException("출석 기록 생성에 실패했습니다."));

                // WebSocket Broadcast
                System.out.println("DEBUG: Broadcasting Student Attendance Update for " + studentQr.getStudent().getName());
                messagingTemplate.convertAndSend("/topic/attendance",
                                new AttendanceUpdateMessage("STUDENT", studentClass.getId(), studentQr.getStudent().getName(),
                                                attendance.getStatus().name(), attendance.getUpdatedAt()));

                return new ScanResponseDto(created ? "SUCCESS" : "DUPLICATE", studentQr.getStudent(), attendance);
        }

        @Transactional
        public boolean upsertAttendance(Long studentClassId, LocalDate date, AttendanceStatus status) {
                StudentClass sc = studentClassRepository.findById(studentClassId)
                                .orElseThrow(() -> new IllegalArgumentException("학생-반 매핑을 찾을 수 없습니다."));

                // 1) 기존에 있으면 상태만 업데이트
                Optional<Attendance> opt = attendanceRepository.findByStudentClassIdAndDate(studentClassId, date);
                if (opt.isPresent()) {
                        opt.get().setStatus(status);

                        // WebSocket Broadcast
                        System.out.println("DEBUG: Broadcasting Student Attendance Update (Update) for " + sc.getStudent().getName());
                        messagingTemplate.convertAndSend("/topic/attendance",
                                        new AttendanceUpdateMessage("STUDENT", sc.getId(), sc.getStudent().getName(), status.name(),
                                                        opt.get().getUpdatedAt()));

                        return false; // 수정
                }

                // 2) 없으면 새로 생성
                Attendance att = new Attendance();
                att.setStudentClass(sc);
                att.setDate(date);
                att.setStatus(status);
                attendanceRepository.save(att);

                // WebSocket Broadcast
                System.out.println("DEBUG: Broadcasting Student Attendance Update (New) for " + sc.getStudent().getName());
                messagingTemplate.convertAndSend("/topic/attendance",
                                new AttendanceUpdateMessage("STUDENT", sc.getId(), sc.getStudent().getName(), status.name(),
                                                att.getUpdatedAt()));

                return true; // 생성
        }

        public List<Attendance> findByStudentClass(Long studentClassId) {
                return attendanceRepository.findAllByStudentClassId(studentClassId);
        }

        @Transactional(readOnly = true)
        public List<StudentAttendanceResponse> findStudentAttendances(Long classRoomId, Integer schoolYear,
                        LocalDate date) {
                List<StudentClass> studentClasses = studentClassRepository.findAllByClassRoomIdAndSchoolYear(
                                classRoomId,
                                schoolYear);

                List<Attendance> attendances = attendanceRepository.findAllByStudentClassInAndDate(studentClasses,
                                date);
                Map<Long, Attendance> attendanceByStudentClassId = attendances.stream()
                                .collect(Collectors.toMap(att -> att.getStudentClass().getId(), att -> att));

                return studentClasses.stream()
                                .map(sc -> {
                                        String status = Optional.ofNullable(attendanceByStudentClassId.get(sc.getId()))
                                                        .map(a -> a.getStatus().name())
                                                        .orElse("UNCHECKED");

                                        return new StudentAttendanceResponse(
                                                        sc.getStudent().getId(),
                                                        sc.getStudent().getName(),
                                                        status);
                                })
                                .collect(Collectors.toList());
        }

        public List<StudentAttendanceResponse> findYearAttendances(int schoolYear, LocalDate date) {
                // 1) 해당 학년도 전체 학생반 조회
                List<StudentClass> all = studentClassRepository.findAllBySchoolYear(schoolYear);

                // 2) N+1 해결을 위해 출석 정보 한번에 로드
                List<Attendance> attendances = attendanceRepository.findAllByStudentClassInAndDate(all, date);
                Map<Long, Attendance> attendanceByStudentClassId = attendances.stream()
                                .collect(Collectors.toMap(att -> att.getStudentClass().getId(), att -> att));

                // 3) 각 학생반마다 date 기준 출석 조회 & DTO 변환
                return all.stream()
                                .map(sc -> {
                                        String status = Optional.ofNullable(attendanceByStudentClassId.get(sc.getId()))
                                                        .map(a -> a.getStatus().name())
                                                        .orElse("UNCHECKED");

                                        return new StudentAttendanceResponse(
                                                        sc.getStudent().getId(),
                                                        sc.getStudent().getName(),
                                                        status);
                                })
                                .collect(Collectors.toList());
        }

        /**
         * classRoomId+date로 학년도별 StudentClass 조회 → 각 학생의 출석 상태 반환
         */
        public List<StudentAttendanceResponse> findStudentAttendancesByClassAndDate(
                        Long classRoomId,
                        LocalDate date) {
                // 1) date 기준 학년도 계산 (3월 시작 가정)
                int schoolYear = date.getMonthValue() >= 3
                                ? date.getYear()
                                : date.getYear() - 1;

                // 2) 해당 학년도, 해당 반에 속한 studentClass 모두 조회
                List<StudentClass> scList = studentClassRepository.findAllByClassRoom_IdAndSchoolYear(classRoomId,
                                schoolYear);

                // 3) N+1 해결을 위해 출석 정보 한번에 로드
                List<Attendance> attendances = attendanceRepository.findAllByStudentClassInAndDate(scList, date);
                Map<Long, Attendance> attendanceByStudentClassId = attendances.stream()
                                .collect(Collectors.toMap(att -> att.getStudentClass().getId(), att -> att));

                // 4) 각 studentClass별로 attendance 조회 후 DTO 변환
                return scList.stream()
                                .map(sc -> {
                                        String status = Optional.ofNullable(attendanceByStudentClassId.get(sc.getId()))
                                                        .map(a -> a.getStatus().name())
                                                        .orElse("UNCHECKED");
                                        return new StudentAttendanceResponse(
                                                        sc.getStudent().getId(),
                                                        sc.getStudent().getName(),
                                                        status);
                                })
                                .collect(Collectors.toList());
        }

        public List<SundayAttendanceSummaryResponse> getSundayAttendanceSummary() {
                List<LocalDate> sundays = attendanceRepository.findDistinctSundays();
                long totalStudentCount = studentRepository.count();

                return sundays.stream()
                                .map(sunday -> {
                                        long attendedCount = attendanceRepository.countByDateAndStatus(sunday,
                                                        AttendanceStatus.ATTEND);
                                        attendedCount += attendanceRepository.countByDateAndStatus(sunday,
                                                        AttendanceStatus.LATE);
                                        return new SundayAttendanceSummaryResponse(sunday, attendedCount,
                                                        totalStudentCount);
                                })
                                .collect(Collectors.toList());
        }

        public List<ClassSundayAttendanceResponse> getSundayAttendanceSummaryForClass(Long classRoomId) {
                // 1. Get all Sundays from attendance data
                List<LocalDate> sundays = attendanceRepository.findDistinctSundays();

                return sundays.stream()
                                .map(sunday -> {
                                        // 2. For each Sunday, calculate schoolYear
                                        int schoolYear = sunday.getMonthValue() >= 3 ? sunday.getYear()
                                                        : sunday.getYear() - 1;

                                        // 3. Get total students in the class for that school year
                                        long totalCount = studentClassRepository
                                                        .countByClassRoomIdAndSchoolYear(classRoomId, schoolYear);

                                        // 4. Get attended students
                                        long attendedCount = attendanceRepository
                                                        .countByClassRoomIdAndDateAndStatusIn(classRoomId, sunday);

                                        return new ClassSundayAttendanceResponse(sunday, attendedCount, totalCount);
                                })
                                .collect(Collectors.toList());
        }

        public List<ClassAttendanceResponse> getAttendanceByClassForDateAndYear(Integer schoolYear, LocalDate date) {
                // 1. 해당 schoolYear의 모든 StudentClass 매핑을 가져옵니다.
                List<StudentClass> studentClasses = studentClassRepository.findAllBySchoolYear(schoolYear);

                // 2. ClassRoom별로 학생들의 출석 정보를 그룹화합니다.
                Map<ClassRoom, List<StudentClass>> studentClassesByRoom = studentClasses.stream()
                                .collect(Collectors.groupingBy(StudentClass::getClassRoom));

                // 3. N+1 해결을 위해 선생님 정보와 출석 정보를 한번에 로드
                List<ClassRoom> classRooms = studentClassesByRoom.keySet().stream().toList();
                List<TeacherClass> teacherClasses = teacherClassRepository.findAllBySchoolYearAndClassRoomIn(schoolYear,
                                classRooms);
                Map<Long, Teacher> teacherByClassRoomId = teacherClasses.stream()
                                .filter(tc -> tc.getTeacher().getDeletedAt() == null)
                                .collect(Collectors.toMap(
                                                tc -> tc.getClassRoom().getId(),
                                                TeacherClass::getTeacher));

                List<Attendance> attendances = attendanceRepository.findAllByStudentClassInAndDate(studentClasses,
                                date);
                Map<Long, Attendance> attendanceByStudentClassId = attendances.stream()
                                .collect(Collectors.toMap(att -> att.getStudentClass().getId(), att -> att));

                // 4. 각 ClassRoom에 대해 ClassAttendanceResponse를 생성합니다.
                return studentClassesByRoom.entrySet().stream()
                                .map(entry -> {
                                        ClassRoom classRoom = entry.getKey();
                                        List<StudentClass> studentsInClass = entry.getValue();

                                        // 5. 해당 ClassRoom의 선생님 이름을 찾습니다.
                                        String teacherName = Optional
                                                        .ofNullable(teacherByClassRoomId.get(classRoom.getId()))
                                                        .map(Teacher::getName)
                                                        .orElse("담당 선생님 없음");

                                        // 6. 학생별 출석 상태를 가져옵니다.
                                        List<StudentAttendanceStatusDto> studentAttendanceStatuses = studentsInClass
                                                        .stream()
                                                        .map(sc -> {
                                                                AttendanceStatus status = Optional
                                                                                .ofNullable(attendanceByStudentClassId
                                                                                                .get(sc.getId()))
                                                                                .map(Attendance::getStatus)
                                                                                .orElse(AttendanceStatus.UNCHECKED); // 출석
                                                                                                                     // 기록이
                                                                                                                     // 없으면
                                                                                                                     // UNCHECKED

                                                                return new StudentAttendanceStatusDto(
                                                                                sc.getId(),
                                                                                sc.getStudent().getName(),
                                                                                status);
                                                        })
                                                        // 학생 이름순으로 정렬
                                                        .sorted(Comparator.comparing(
                                                                        StudentAttendanceStatusDto::getStudentName))
                                                        .collect(Collectors.toList());

                                        // 7. ClassAttendanceResponse 객체를 생성합니다.
                                        return new ClassAttendanceResponse(
                                                        classRoom.getId(),
                                                        classRoom.getName(),
                                                        teacherName,
                                                        studentAttendanceStatuses);
                                })
                                // 반 이름 또는 학년-반 번호 순으로 정렬
                                .sorted(Comparator
                                                .comparing(ClassAttendanceResponse::getClassName)) // Assuming
                                                                                                   // classRoom.getName()
                                                                                                   // provides a
                                                                                                   // sortable order
                                .collect(Collectors.toList());
        }

        public List<GradeSundayAttendanceResponse> getGradeSundayAttendanceSummary() {
                // 1. 최근 5개 일요일 목록 조회
                LocalDate today = LocalDate.now();

                List<LocalDate> recentSundays = attendanceRepository.findDistinctSundays()
                                .stream()
                                .filter(sunday -> !sunday.isAfter(today))
                                .limit(5) // 최근 5개만
                                .sorted(Comparator.reverseOrder()) // 최신순 정렬
                                .collect(Collectors.toList());

                if (recentSundays.isEmpty()) {
                        return Collections.emptyList();
                }

                // 2. 학년도 목록 계산 (1~2월도 현재 연도 학년도로 처리)
                Set<Integer> schoolYears = recentSundays.stream()
                                .map(sunday -> sunday.getYear())
                                .collect(Collectors.toSet());

                // 3. 한 번에 모든 출석 데이터 조회 (1번의 쿼리)
                List<AttendanceRepository.GradeAttendanceProjection> attendanceStats = attendanceRepository
                                .findGradeAttendanceStats(recentSundays);

                // 4. 한 번에 모든 학생 수 조회 (1번의 쿼리)
                List<StudentClassRepository.GradeStudentCountProjection> studentCounts = studentClassRepository
                                .findGradeStudentCounts(new ArrayList<>(schoolYears));

                // 5. Map으로 변환하여 빠른 조회
                Map<String, Long> attendanceMap = attendanceStats.stream()
                                .collect(Collectors.toMap(
                                                stat -> String.format("%s_%d_%s_%d",
                                                                stat.getSchoolType(), stat.getGrade(), stat.getDate(),
                                                                stat.getSchoolYear()),
                                                AttendanceRepository.GradeAttendanceProjection::getAttendedCount));

                Map<String, Long> studentCountMap = studentCounts.stream()
                                .collect(Collectors.toMap(
                                                count -> String.format("%s_%d_%d",
                                                                count.getSchoolType(), count.getGrade(),
                                                                count.getSchoolYear()),
                                                StudentClassRepository.GradeStudentCountProjection::getTotalCount));

                // 6. 모든 학년 조합 생성 (중1, 중2, 중3, 고1, 고2, 고3)
                List<GradeInfo> allGrades = Arrays.asList(
                                new GradeInfo(ClassRoom.SchoolType.MIDDLE, 1),
                                new GradeInfo(ClassRoom.SchoolType.MIDDLE, 2),
                                new GradeInfo(ClassRoom.SchoolType.MIDDLE, 3),
                                new GradeInfo(ClassRoom.SchoolType.HIGH, 1),
                                new GradeInfo(ClassRoom.SchoolType.HIGH, 2),
                                new GradeInfo(ClassRoom.SchoolType.HIGH, 3));

                // 7. 각 학년별로 일요일 통계 계산 (추가 쿼리 없음!)
                return allGrades.stream()
                                .map(gradeInfo -> {
                                        List<SundayStatDto> sundayStats = recentSundays.stream()
                                                        .map(sunday -> {
                                                                // 1~2월도 현재 연도 학년도로 처리
                                                                int schoolYear = sunday.getYear();

                                                                String attendanceKey = String.format("%s_%d_%s_%d",
                                                                                gradeInfo.schoolType, gradeInfo.grade,
                                                                                sunday, schoolYear);
                                                                String studentCountKey = String.format("%s_%d_%d",
                                                                                gradeInfo.schoolType, gradeInfo.grade,
                                                                                schoolYear);

                                                                long attendedCount = attendanceMap
                                                                                .getOrDefault(attendanceKey, 0L);
                                                                long totalCount = studentCountMap
                                                                                .getOrDefault(studentCountKey, 0L);

                                                                double attendanceRate = totalCount > 0
                                                                                ? Math.round((double) attendedCount
                                                                                                / totalCount * 1000.0)
                                                                                                / 10.0
                                                                                : 0.0;

                                                                return new SundayStatDto(sunday, attendedCount,
                                                                                totalCount, attendanceRate);
                                                        })
                                                        .collect(Collectors.toList());

                                        String gradeName = (gradeInfo.schoolType == ClassRoom.SchoolType.MIDDLE ? "중 "
                                                        : "고 ")
                                                        + gradeInfo.grade;

                                        return new GradeSundayAttendanceResponse(
                                                        gradeInfo.schoolType,
                                                        gradeInfo.grade,
                                                        gradeName,
                                                        sundayStats);
                                })
                                .collect(Collectors.toList());
        }

        // Helper class for grade information
        private static class GradeInfo {
                ClassRoom.SchoolType schoolType;
                Integer grade;

                GradeInfo(ClassRoom.SchoolType schoolType, Integer grade) {
                        this.schoolType = schoolType;
                        this.grade = grade;
                }
        }

        @lombok.Data
        @lombok.AllArgsConstructor
        public static class AttendanceUpdateMessage {
                private String type; // STUDENT, TEACHER, PARENT
                private Long id; // studentClassId or teacherId or studentId
                private String name;
                private String status;
                @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
                private java.time.LocalDateTime timestamp;
        }
}

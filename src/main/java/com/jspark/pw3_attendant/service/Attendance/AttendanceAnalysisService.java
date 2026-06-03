package com.jspark.pw3_attendant.service.Attendance;

import com.jspark.pw3_attendant.repository.Attendance.AttendanceRepository;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.repository.TeacherClass.TeacherClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import com.jspark.pw3_attendant.domain.TeacherClass.TeacherClass;
import com.jspark.pw3_attendant.service.Attendance.dto.AbsenteeResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.MonthlyClassAttendanceDto;
import com.jspark.pw3_attendant.service.Attendance.dto.MonthlyClassAttendanceReportResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.NewStudentAttendeeResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.WeakClassDto;
import com.jspark.pw3_attendant.service.Attendance.dto.WeeklyClassAttendanceDto;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AttendanceAnalysisService {

        private final StudentRepository studentRepository;
        private final AttendanceRepository attendanceRepository;
        private final StudentClassRepository studentClassRepository;
        private final TeacherClassRepository teacherClassRepository;

        public MonthlyClassAttendanceReportResponse getMonthlyClassAttendanceReport(
                        int year,
                        int month,
                        int schoolYear,
                        double weakClassThreshold) {
                YearMonth targetMonth = YearMonth.of(year, month);
                List<LocalDate> sundays = getSundaysInMonth(targetMonth);

                List<StudentClass> studentClasses = studentClassRepository.findAllBySchoolYear(schoolYear);
                if (studentClasses.isEmpty()) {
                        return MonthlyClassAttendanceReportResponse.builder()
                                        .year(year)
                                        .month(month)
                                        .schoolYear(schoolYear)
                                        .totalSundays(sundays.size())
                                        .sundays(sundays)
                                        .averageAttendanceRate(0.0)
                                        .weakClassThreshold(weakClassThreshold)
                                        .totalClasses(0)
                                        .weakClassCount(0)
                                        .topClasses(List.of())
                                        .classes(List.of())
                                        .weakClasses(List.of())
                                        .build();
                }

                Map<Long, List<StudentClass>> studentClassesByClassRoomId = studentClasses.stream()
                                .collect(Collectors.groupingBy(sc -> sc.getClassRoom().getId()));

                List<TeacherClass> teacherClasses = teacherClassRepository.findAllBySchoolYearAndClassRoomIn(
                                schoolYear,
                                studentClasses.stream()
                                                .map(StudentClass::getClassRoom)
                                                .distinct()
                                                .toList());
                Map<Long, String> teacherNameByClassRoomId = teacherClasses.stream()
                                .filter(tc -> tc.getTeacher().getDeletedAt() == null)
                                .collect(Collectors.toMap(
                                                tc -> tc.getClassRoom().getId(),
                                                tc -> tc.getTeacher().getName(),
                                                (existing, replacement) -> existing));

                Map<String, Long> attendedCountByClassRoomAndDate = getAttendedCountByClassRoomAndDate(
                                targetMonth.atDay(1),
                                targetMonth.atEndOfMonth(),
                                schoolYear);
                Map<Long, Double> previousMonthRateByClassRoomId = getPreviousMonthRateByClassRoomId(
                                targetMonth.minusMonths(1),
                                schoolYear);

                List<MonthlyClassAttendanceDto> classes = studentClassesByClassRoomId.entrySet().stream()
                                .map(entry -> {
                                        Long classRoomId = entry.getKey();
                                        List<StudentClass> classStudents = entry.getValue();
                                        int totalStudents = classStudents.size();
                                        String className = classStudents.get(0).getClassRoom().getName();
                                        String teacherName = teacherNameByClassRoomId.getOrDefault(
                                                        classRoomId,
                                                        "담당 선생님 없음");

                                        List<WeeklyClassAttendanceDto> weeklyStats = sundays.stream()
                                                        .map(sunday -> {
                                                                int attendedCount = attendedCountByClassRoomAndDate
                                                                                .getOrDefault(
                                                                                                buildAttendanceKey(
                                                                                                                classRoomId,
                                                                                                                sunday),
                                                                                                0L)
                                                                                .intValue();
                                                                return WeeklyClassAttendanceDto.builder()
                                                                                .date(sunday)
                                                                                .totalStudents(totalStudents)
                                                                                .attendedCount(attendedCount)
                                                                                .attendanceRate(roundRate(
                                                                                                totalStudents > 0
                                                                                                                ? attendedCount
                                                                                                                                * 100.0
                                                                                                                                / totalStudents
                                                                                                                : 0.0))
                                                                                .build();
                                                        })
                                                        .toList();

                                        double attendanceRate = weeklyStats.isEmpty()
                                                        ? 0.0
                                                        : roundRate(weeklyStats.stream()
                                                                        .mapToDouble(WeeklyClassAttendanceDto::getAttendanceRate)
                                                                        .average()
                                                                        .orElse(0.0));
                                        double averageAttendedCount = weeklyStats.isEmpty()
                                                        ? 0.0
                                                        : roundOneDecimal(weeklyStats.stream()
                                                                        .mapToInt(WeeklyClassAttendanceDto::getAttendedCount)
                                                                        .average()
                                                                        .orElse(0.0));
                                        Double previousRate = previousMonthRateByClassRoomId.get(classRoomId);
                                        Double monthOverMonthChange = previousRate == null
                                                        ? null
                                                        : roundRate(attendanceRate - previousRate);

                                        return MonthlyClassAttendanceDto.builder()
                                                        .classRoomId(classRoomId)
                                                        .className(className)
                                                        .teacherName(teacherName)
                                                        .totalStudents(totalStudents)
                                                        .averageAttendedCount(averageAttendedCount)
                                                        .attendanceRate(attendanceRate)
                                                        .previousMonthAttendanceRate(previousRate)
                                                        .monthOverMonthChange(monthOverMonthChange)
                                                        .rank(0)
                                                        .status(resolveClassStatus(attendanceRate, weakClassThreshold,
                                                                        monthOverMonthChange))
                                                        .weeklyStats(weeklyStats)
                                                        .build();
                                })
                                .sorted(Comparator.comparing(MonthlyClassAttendanceDto::getAttendanceRate).reversed()
                                                .thenComparing(MonthlyClassAttendanceDto::getClassName))
                                .toList();

                List<MonthlyClassAttendanceDto> rankedClasses = new ArrayList<>();
                for (int i = 0; i < classes.size(); i++) {
                        MonthlyClassAttendanceDto item = classes.get(i);
                        rankedClasses.add(MonthlyClassAttendanceDto.builder()
                                        .classRoomId(item.getClassRoomId())
                                        .className(item.getClassName())
                                        .teacherName(item.getTeacherName())
                                        .totalStudents(item.getTotalStudents())
                                        .averageAttendedCount(item.getAverageAttendedCount())
                                        .attendanceRate(item.getAttendanceRate())
                                        .previousMonthAttendanceRate(item.getPreviousMonthAttendanceRate())
                                        .monthOverMonthChange(item.getMonthOverMonthChange())
                                        .rank(i + 1)
                                        .status(item.getStatus())
                                        .weeklyStats(item.getWeeklyStats())
                                        .build());
                }

                List<WeakClassDto> weakClasses = rankedClasses.stream()
                                .filter(item -> "WEAK".equals(item.getStatus()) || "DROPPED".equals(item.getStatus()))
                                .map(item -> WeakClassDto.builder()
                                                .classRoomId(item.getClassRoomId())
                                                .className(item.getClassName())
                                                .teacherName(item.getTeacherName())
                                                .attendanceRate(item.getAttendanceRate())
                                                .previousMonthAttendanceRate(item.getPreviousMonthAttendanceRate())
                                                .monthOverMonthChange(item.getMonthOverMonthChange())
                                                .reason(buildWeakClassReason(item, weakClassThreshold))
                                                .build())
                                .sorted(Comparator.comparing(WeakClassDto::getAttendanceRate)
                                                .thenComparing(WeakClassDto::getClassName))
                                .toList();

                double averageAttendanceRate = rankedClasses.isEmpty()
                                ? 0.0
                                : roundRate(rankedClasses.stream()
                                                .mapToDouble(MonthlyClassAttendanceDto::getAttendanceRate)
                                                .average()
                                                .orElse(0.0));

                return MonthlyClassAttendanceReportResponse.builder()
                                .year(year)
                                .month(month)
                                .schoolYear(schoolYear)
                                .totalSundays(sundays.size())
                                .sundays(sundays)
                                .averageAttendanceRate(averageAttendanceRate)
                                .weakClassThreshold(weakClassThreshold)
                                .totalClasses(rankedClasses.size())
                                .weakClassCount(weakClasses.size())
                                .topClasses(rankedClasses.stream().limit(3).toList())
                                .classes(rankedClasses)
                                .weakClasses(weakClasses)
                                .build();
        }

        public List<AbsenteeResponse> findLongTermAbsentees(LocalDate startDate, LocalDate endDate) {
                int currentYear = getCurrentYear();

                // 1. 현재 연도에 등록된 모든 학생들을 조회합니다.
                List<StudentClass> allStudentClasses = studentClassRepository.findAllBySchoolYear(currentYear);
                List<Student> allStudents = allStudentClasses.stream()
                                .map(StudentClass::getStudent)
                                .distinct()
                                .toList();

                // 2. 주어진 기간 동안 출석한 학생들의 ID 목록을 조회합니다.
                List<Attendance> attendances = attendanceRepository.findSundayAttendanceByDateBetweenAndStatusIn(
                                startDate, endDate,
                                List.of(Attendance.AttendanceStatus.ATTEND, Attendance.AttendanceStatus.LATE));

                List<Long> attendedStudentIds = attendances.stream()
                                .map(att -> att.getStudentClass().getStudent().getId())
                                .distinct()
                                .toList();

                // 3. 전체 학생 중 출석 기록이 없는 학생(장기 결석자)을 필터링합니다.
                List<Student> absenteeStudents = allStudents.stream()
                                .filter(student -> !attendedStudentIds.contains(student.getId()))
                                .toList();

                // 4. 결석한 학생들의 반 정보를 조회하고 DTO로 변환합니다.
                Map<Long, String> classNameByStudentId = allStudentClasses.stream()
                                .collect(Collectors.toMap(
                                                sc -> sc.getStudent().getId(),
                                                sc -> sc.getClassRoom().getName(),
                                                (existing, replacement) -> existing // 중복 키의 경우 기존 값 유지
                                ));

                return absenteeStudents.stream()
                                .map(student -> new AbsenteeResponse(
                                                student.getId(),
                                                student.getName(),
                                                classNameByStudentId.getOrDefault(student.getId(), "배정반 없음"),
                                                student.getPhone(),
                                                student.getParentPhone()))
                                .collect(Collectors.toList());
        }

        public List<AbsenteeResponse> findConsecutiveAbsenceStudents(int weeks) {
                int currentYear = getCurrentYear();
                LocalDate today = LocalDate.now();

                // 1. 현재 연도에 등록된 모든 학생들을 조회합니다.
                List<StudentClass> allStudentClasses = studentClassRepository.findAllBySchoolYear(currentYear);
                List<Student> allStudents = allStudentClasses.stream()
                                .map(StudentClass::getStudent)
                                .distinct()
                                .toList();
                Set<Long> allStudentIds = allStudents.stream().map(Student::getId).collect(Collectors.toSet());

                // 2. 지난 N주 동안 매주 결석한 학생을 찾습니다.
                Set<Long> consecutiveAbsentees = new HashSet<>(allStudentIds);

                for (int i = 0; i < weeks; i++) {
                        // 가장 최근 일요일부터 역순으로 계산
                        LocalDate sunday = today.minusWeeks(i);
                        // 이번 주 일요일이 미래라면 지난 주 일요일로 조정
                        while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
                                sunday = sunday.minusDays(1);
                        }
                        LocalDate weekStart = sunday;
                        LocalDate weekEnd = sunday;

                        List<Attendance> weeklyAttendances = attendanceRepository
                                        .findSundayAttendanceByDateBetweenAndStatusIn(weekStart, weekEnd,
                                                        List.of(Attendance.AttendanceStatus.ATTEND,
                                                                        Attendance.AttendanceStatus.LATE));

                        Set<Long> weeklyAttendees = weeklyAttendances.stream()
                                        .map(att -> att.getStudentClass().getStudent().getId())
                                        .collect(Collectors.toSet());

                        // 해당 주에 결석한 학생들 = 전체 학생 - 출석한 학생
                        Set<Long> weeklyAbsentees = new HashSet<>(allStudentIds);
                        weeklyAbsentees.removeAll(weeklyAttendees);

                        // 연속 결석자 목록을 갱신합니다 (교집합).
                        consecutiveAbsentees.retainAll(weeklyAbsentees);
                }

                // 3. 최종 결석자 목록을 학생 정보로 변환합니다.
                List<Student> finalAbsentees = allStudents.stream()
                                .filter(student -> consecutiveAbsentees.contains(student.getId()))
                                .toList();

                // 4. 결석한 학생들의 반 정보를 조회하고 DTO로 변환합니다.
                Map<Long, String> classNameByStudentId = allStudentClasses.stream()
                                .collect(Collectors.toMap(
                                                sc -> sc.getStudent().getId(),
                                                sc -> sc.getClassRoom().getName(),
                                                (existing, replacement) -> existing));

                return finalAbsentees.stream()
                                .map(student -> new AbsenteeResponse(
                                                student.getId(),
                                                student.getName(),
                                                classNameByStudentId.getOrDefault(student.getId(), "배정반 없음"),
                                                student.getPhone(),
                                                student.getParentPhone()))
                                .collect(Collectors.toList());
        }

        public List<NewStudentAttendeeResponse> findNewConsecutiveAttendees(int weeks, int months) {
                int currentYear = getCurrentYear();
                LocalDate today = LocalDate.now();
                LocalDateTime registrationCutoff = today.minusMonths(months).atStartOfDay();

                // 1. 최근 N개월 내 등록한 신입생 목록을 조회합니다.
                List<Student> newStudents = studentRepository.findAllByCreatedAtAfter(registrationCutoff);
                Set<Long> newStudentIds = newStudents.stream().map(Student::getId).collect(Collectors.toSet());

                // 2. 신입생 중 현재 연도에 등록된 학생들을 필터링합니다.
                List<StudentClass> allStudentClasses = studentClassRepository.findAllBySchoolYear(currentYear);
                Map<Long, String> classNameByStudentId = allStudentClasses.stream()
                                .collect(Collectors.toMap(
                                                sc -> sc.getStudent().getId(),
                                                sc -> sc.getClassRoom().getName(),
                                                (existing, replacement) -> existing));

                Set<Long> currentYearNewStudentIds = newStudentIds.stream()
                                .filter(classNameByStudentId::containsKey)
                                .collect(Collectors.toSet());

                // 3. 지난 N주 동안 매주 출석한 신입생을 찾습니다.
                Set<Long> consecutiveAttendees = new HashSet<>(currentYearNewStudentIds);

                for (int i = 0; i < weeks; i++) {
                        // 가장 최근 일요일부터 역순으로 계산
                        LocalDate sunday = today.minusWeeks(i);
                        // 이번 주 일요일이 미래라면 지난 주 일요일로 조정
                        while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
                                sunday = sunday.minusDays(1);
                        }
                        LocalDate weekStart = sunday;
                        LocalDate weekEnd = sunday;

                        List<Attendance> weeklyAttendances = attendanceRepository
                                        .findSundayAttendanceByDateBetweenAndStatusIn(weekStart, weekEnd,
                                                        List.of(Attendance.AttendanceStatus.ATTEND,
                                                                        Attendance.AttendanceStatus.LATE));

                        Set<Long> weeklyAttendees = weeklyAttendances.stream()
                                        .map(att -> att.getStudentClass().getStudent().getId())
                                        .collect(Collectors.toSet());

                        // 연속 출석자 목록을 갱신합니다 (교집합).
                        consecutiveAttendees.retainAll(weeklyAttendees);
                }

                // 4. 최종 출석자 목록을 학생 정보로 변환합니다.
                List<Student> finalAttendees = newStudents.stream()
                                .filter(student -> consecutiveAttendees.contains(student.getId()))
                                .toList();

                return finalAttendees.stream()
                                .map(student -> new NewStudentAttendeeResponse(
                                                student.getId(),
                                                student.getName(),
                                                classNameByStudentId.getOrDefault(student.getId(), "배정반 없음"),
                                                student.getPhone(),
                                                student.getParentPhone()))
                                .collect(Collectors.toList());
        }

        /**
         * 학년별 평균 출석률 계산
         * 
         * @param startDate  시작 날짜
         * @param endDate    종료 날짜
         * @param schoolYear 학년도
         * @return 학년별 출석률 목록
         */
        public List<com.jspark.pw3_attendant.service.Attendance.dto.GradeAttendanceRateDto> getAverageAttendanceRateByGrade(
                        LocalDate startDate, LocalDate endDate, Integer schoolYear) {

                // 1. 해당 학년도의 모든 학생-반 매핑 조회
                List<StudentClass> allStudentClasses = studentClassRepository.findAllBySchoolYear(schoolYear);

                // 2. 학년별로 그룹화 (중1, 중2, 중3, 고1, 고2, 고3)
                Map<String, List<StudentClass>> studentClassesByGrade = allStudentClasses.stream()
                                .collect(Collectors.groupingBy(sc -> sc.getClassRoom().getName().split("-")[0].trim()));

                // 3. 각 학년별 출석률 계산
                List<com.jspark.pw3_attendant.service.Attendance.dto.GradeAttendanceRateDto> results = new ArrayList<>();

                for (Map.Entry<String, List<StudentClass>> entry : studentClassesByGrade.entrySet()) {
                        String gradeName = entry.getKey();
                        List<StudentClass> gradeStudentClasses = entry.getValue();
                        int totalStudents = gradeStudentClasses.size();

                        // 기간 내 출석/지각 기록 조회
                        List<Attendance> attendances = attendanceRepository
                                        .findSundayAttendanceByDateBetweenAndStatusIn(
                                                        startDate, endDate,
                                                        List.of(Attendance.AttendanceStatus.ATTEND,
                                                                        Attendance.AttendanceStatus.LATE));

                        // 해당 학년 학생들의 출석 기록만 필터링
                        Set<Long> gradeStudentClassIds = gradeStudentClasses.stream()
                                        .map(StudentClass::getId)
                                        .collect(Collectors.toSet());

                        long attendedCount = attendances.stream()
                                        .filter(att -> gradeStudentClassIds.contains(att.getStudentClass().getId()))
                                        .map(att -> att.getStudentClass().getStudent().getId())
                                        .distinct()
                                        .count();

                        double attendanceRate = totalStudents > 0 ? (attendedCount * 100.0 / totalStudents) : 0.0;

                        results.add(new com.jspark.pw3_attendant.service.Attendance.dto.GradeAttendanceRateDto(
                                        gradeName,
                                        Math.round(attendanceRate * 100.0) / 100.0, // 소수점 2자리
                                        totalStudents,
                                        (int) attendedCount));
                }

                return results;
        }

        /**
         * 관리가 필요한 학생 찾기 (결석 2회 이상 또는 지각 3회 이상)
         * 
         * @param startDate  시작 날짜
         * @param endDate    종료 날짜
         * @param schoolYear 학년도
         * @return 관리가 필요한 학생 목록
         */
        public List<com.jspark.pw3_attendant.service.Attendance.dto.StudentNeedsCareDto> findStudentsNeedingCare(
                        LocalDate startDate, LocalDate endDate, Integer schoolYear) {

                // 1. 해당 학년도의 모든 학생-반 매핑 조회
                List<StudentClass> allStudentClasses = studentClassRepository.findAllBySchoolYear(schoolYear);

                // 2. 기간 내 모든 출석 기록 조회
                List<Attendance> allAttendances = attendanceRepository.findSundayAttendanceByDateBetweenAndStatusIn(
                                startDate, endDate,
                                List.of(Attendance.AttendanceStatus.ABSENT, Attendance.AttendanceStatus.LATE));

                // 3. 학생별 결석/지각 횟수 집계
                Map<Long, Long> absenceCountByStudent = allAttendances.stream()
                                .filter(att -> att.getStatus() == Attendance.AttendanceStatus.ABSENT)
                                .collect(Collectors.groupingBy(
                                                att -> att.getStudentClass().getStudent().getId(),
                                                Collectors.counting()));

                Map<Long, Long> lateCountByStudent = allAttendances.stream()
                                .filter(att -> att.getStatus() == Attendance.AttendanceStatus.LATE)
                                .collect(Collectors.groupingBy(
                                                att -> att.getStudentClass().getStudent().getId(),
                                                Collectors.counting()));

                // 4. 관리가 필요한 학생 필터링 (결석 2회 이상 또는 지각 3회 이상)
                Map<Long, String> classNameByStudentId = allStudentClasses.stream()
                                .collect(Collectors.toMap(
                                                sc -> sc.getStudent().getId(),
                                                sc -> sc.getClassRoom().getName(),
                                                (existing, replacement) -> existing));

                List<com.jspark.pw3_attendant.service.Attendance.dto.StudentNeedsCareDto> results = new ArrayList<>();

                Set<Long> processedStudents = new HashSet<>();

                // 결석 2회 이상 학생
                for (Map.Entry<Long, Long> entry : absenceCountByStudent.entrySet()) {
                        if (entry.getValue() >= 2) {
                                Long studentId = entry.getKey();
                                Student student = allStudentClasses.stream()
                                                .filter(sc -> sc.getStudent().getId().equals(studentId))
                                                .map(StudentClass::getStudent)
                                                .findFirst()
                                                .orElse(null);

                                if (student != null) {
                                        results.add(new com.jspark.pw3_attendant.service.Attendance.dto.StudentNeedsCareDto(
                                                        studentId,
                                                        student.getName(),
                                                        classNameByStudentId.getOrDefault(studentId, "배정반 없음"),
                                                        entry.getValue().intValue(),
                                                        lateCountByStudent.getOrDefault(studentId, 0L).intValue(),
                                                        "결석 " + entry.getValue() + "회"));
                                        processedStudents.add(studentId);
                                }
                        }
                }

                // 지각 3회 이상 학생 (결석으로 이미 추가되지 않은 경우만)
                for (Map.Entry<Long, Long> entry : lateCountByStudent.entrySet()) {
                        if (entry.getValue() >= 3 && !processedStudents.contains(entry.getKey())) {
                                Long studentId = entry.getKey();
                                Student student = allStudentClasses.stream()
                                                .filter(sc -> sc.getStudent().getId().equals(studentId))
                                                .map(StudentClass::getStudent)
                                                .findFirst()
                                                .orElse(null);

                                if (student != null) {
                                        results.add(new com.jspark.pw3_attendant.service.Attendance.dto.StudentNeedsCareDto(
                                                        studentId,
                                                        student.getName(),
                                                        classNameByStudentId.getOrDefault(studentId, "배정반 없음"),
                                                        absenceCountByStudent.getOrDefault(studentId, 0L).intValue(),
                                                        entry.getValue().intValue(),
                                                        "지각 " + entry.getValue() + "회"));
                                }
                        }
                }

                return results;
        }

        /**
         * 지각 빈도가 높은 학생 조회
         * 
         * @param topN      상위 N명
         * @param startDate 시작 날짜
         * @param endDate   종료 날짜
         * @return 지각 빈도 높은 학생 목록
         */
        public List<com.jspark.pw3_attendant.service.Attendance.dto.StudentLatenessDto> findFrequentLateStudents(
                        int topN, LocalDate startDate, LocalDate endDate) {

                int currentYear = getCurrentYear();

                // 1. 기간 내 지각 기록 조회
                List<Attendance> lateAttendances = attendanceRepository.findSundayAttendanceByDateBetweenAndStatusIn(
                                startDate, endDate,
                                List.of(Attendance.AttendanceStatus.LATE));

                // 2. 학생별 지각 횟수 집계
                Map<Long, Long> lateCountByStudent = lateAttendances.stream()
                                .collect(Collectors.groupingBy(
                                                att -> att.getStudentClass().getStudent().getId(),
                                                Collectors.counting()));

                // 3. 학생 정보 및 반 정보 조회
                List<StudentClass> allStudentClasses = studentClassRepository.findAllBySchoolYear(currentYear);
                Map<Long, String> classNameByStudentId = allStudentClasses.stream()
                                .collect(Collectors.toMap(
                                                sc -> sc.getStudent().getId(),
                                                sc -> sc.getClassRoom().getName(),
                                                (existing, replacement) -> existing));

                Map<Long, Student> studentById = allStudentClasses.stream()
                                .collect(Collectors.toMap(
                                                sc -> sc.getStudent().getId(),
                                                StudentClass::getStudent,
                                                (existing, replacement) -> existing));

                // 4. 지각 횟수 내림차순 정렬 후 상위 N명 추출
                return lateCountByStudent.entrySet().stream()
                                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                                .limit(topN)
                                .map(entry -> {
                                        Long studentId = entry.getKey();
                                        Student student = studentById.get(studentId);
                                        return new com.jspark.pw3_attendant.service.Attendance.dto.StudentLatenessDto(
                                                        studentId,
                                                        student != null ? student.getName() : "알 수 없음",
                                                        classNameByStudentId.getOrDefault(studentId, "배정반 없음"),
                                                        entry.getValue().intValue());
                                })
                                .collect(Collectors.toList());
        }

        private int getCurrentYear() {
                return LocalDate.now().getYear();
        }

        private Map<String, Long> getAttendedCountByClassRoomAndDate(LocalDate startDate, LocalDate endDate,
                        int schoolYear) {
                List<Attendance> attendances = attendanceRepository
                                .findByDateBetweenAndSchoolYearAndStatusInWithStudentClass(
                                startDate,
                                endDate,
                                schoolYear,
                                List.of(Attendance.AttendanceStatus.ATTEND, Attendance.AttendanceStatus.LATE));

                return attendances.stream()
                                .collect(Collectors.groupingBy(
                                                attendance -> buildAttendanceKey(
                                                                attendance.getStudentClass().getClassRoom().getId(),
                                                                attendance.getDate()),
                                                Collectors.counting()));
        }

        private Map<Long, Double> getPreviousMonthRateByClassRoomId(YearMonth previousMonth, int schoolYear) {
                List<LocalDate> previousSundays = getSundaysInMonth(previousMonth);
                if (previousSundays.isEmpty()) {
                        return Map.of();
                }

                List<StudentClass> studentClasses = studentClassRepository.findAllBySchoolYear(schoolYear);
                Map<Long, Long> totalStudentsByClassRoomId = studentClasses.stream()
                                .collect(Collectors.groupingBy(
                                                sc -> sc.getClassRoom().getId(),
                                                Collectors.counting()));

                Map<String, Long> attendedCountByClassRoomAndDate = getAttendedCountByClassRoomAndDate(
                                previousMonth.atDay(1),
                                previousMonth.atEndOfMonth(),
                                schoolYear);

                return totalStudentsByClassRoomId.entrySet().stream()
                                .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                entry -> {
                                                        Long classRoomId = entry.getKey();
                                                        long totalStudents = entry.getValue();
                                                        return roundRate(previousSundays.stream()
                                                                        .mapToDouble(sunday -> {
                                                                                long attendedCount = attendedCountByClassRoomAndDate
                                                                                                .getOrDefault(
                                                                                                                buildAttendanceKey(
                                                                                                                                classRoomId,
                                                                                                                                sunday),
                                                                                                                0L);
                                                                                return totalStudents > 0
                                                                                                ? attendedCount * 100.0
                                                                                                                / totalStudents
                                                                                                : 0.0;
                                                                        })
                                                                        .average()
                                                                        .orElse(0.0));
                                                }));
        }

        private List<LocalDate> getSundaysInMonth(YearMonth yearMonth) {
                List<LocalDate> sundays = new ArrayList<>();
                LocalDate date = yearMonth.atDay(1);
                while (!date.isAfter(yearMonth.atEndOfMonth())) {
                        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                                sundays.add(date);
                        }
                        date = date.plusDays(1);
                }
                return sundays;
        }

        private String buildAttendanceKey(Long classRoomId, LocalDate date) {
                return classRoomId + "_" + date;
        }

        private String resolveClassStatus(double attendanceRate, double weakClassThreshold, Double monthOverMonthChange) {
                if (attendanceRate < weakClassThreshold) {
                        return "WEAK";
                }
                if (monthOverMonthChange != null && monthOverMonthChange <= -10.0) {
                        return "DROPPED";
                }
                if (attendanceRate >= 85.0) {
                        return "GOOD";
                }
                return "NORMAL";
        }

        private String buildWeakClassReason(MonthlyClassAttendanceDto item, double weakClassThreshold) {
                if ("WEAK".equals(item.getStatus())) {
                        return "월 평균 출석률 " + item.getAttendanceRate() + "%로 기준치 "
                                        + weakClassThreshold + "% 미만";
                }
                if ("DROPPED".equals(item.getStatus())) {
                        return "전월 대비 " + Math.abs(item.getMonthOverMonthChange()) + "%p 하락";
                }
                return "관리 확인 필요";
        }

        private double roundRate(double value) {
                return Math.round(value * 10.0) / 10.0;
        }

        private double roundOneDecimal(double value) {
                return Math.round(value * 10.0) / 10.0;
        }

}

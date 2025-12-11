package com.jspark.pw3_attendant.service.Attendance;


import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;

import com.jspark.pw3_attendant.domain.student_qr.StudentQr;
import com.jspark.pw3_attendant.repository.Attendance.AttendanceRepository;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.repository.TeacherClass.TeacherClassRepository;
import com.jspark.pw3_attendant.repository.student_qr.StudentQrRepository;
import com.jspark.pw3_attendant.service.Attendance.dto.ClassAttendanceResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.StudentAttendanceStatusDto;
import com.jspark.pw3_attendant.service.Attendance.dto.ClassSundayAttendanceResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.StudentAttendanceResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.SundayAttendanceSummaryResponse;
import com.jspark.pw3_attendant.service.attendance.dto.ScanResponseDto;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentClassRepository studentClassRepository;
    private final StudentRepository studentRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final StudentQrRepository studentQrRepository;


    @Transactional
    public ScanResponseDto processScan(
        com.jspark.pw3_attendant.service.attendance.dto.ScanRequestDto request) {
        // 1. Parse qrPayload
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
        // TODO: Add logic for attendance time validation (e.g., only within class hours).
        boolean created = upsertAttendance(studentClass.getId(), LocalDate.now(), AttendanceStatus.ATTEND);
        Attendance attendance = attendanceRepository.findByStudentClassIdAndDate(studentClass.getId(), LocalDate.now())
            .orElseThrow(() -> new IllegalStateException("출석 기록 생성에 실패했습니다."));

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
            return false;  // 수정
        }

        // 2) 없으면 새로 생성
        Attendance att = new Attendance();
        att.setStudentClass(sc);
        att.setDate(date);
        att.setStatus(status);
        attendanceRepository.save(att);
        return true;   // 생성
    }

    public List<Attendance> findByStudentClass(Long studentClassId) {
        return attendanceRepository.findAllByStudentClassId(studentClassId);
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceResponse> findStudentAttendances(Long classRoomId, Integer schoolYear, LocalDate date) {
        List<StudentClass> studentClasses = studentClassRepository.findAllByClassRoomIdAndSchoolYear(classRoomId, schoolYear);

        List<StudentAttendanceResponse> result = new ArrayList<>();

        for (StudentClass studentClass : studentClasses) {
            Optional<Attendance> attendanceOpt = attendanceRepository.findByStudentClassIdAndDate(studentClass.getId(), date);

            String status = attendanceOpt
                .map(attendance -> attendance.getStatus().name()) // 출석 있으면 상태
                .orElse("UNCHECKED"); // 출석 기록 없으면 "미체크"

            result.add(new StudentAttendanceResponse(
                studentClass.getStudent().getId(),
                studentClass.getStudent().getName(),
                status
            ));
        }

        return result;
    }

    public List<StudentAttendanceResponse> findYearAttendances(int schoolYear, LocalDate date) {
        // 1) 해당 학년도 전체 학생반 조회
        List<StudentClass> all = studentClassRepository.findAllBySchoolYear(schoolYear);

        // 2) 각 학생반마다 date 기준 출석 조회 & DTO 변환
        List<StudentAttendanceResponse> result = new ArrayList<>();
        for (StudentClass sc : all) {
            Optional<Attendance> att = attendanceRepository
                .findByStudentClassIdAndDate(sc.getId(), date);

            String status = att
                .map(a -> a.getStatus().name())
                .orElse("UNCHECKED");

            result.add(new StudentAttendanceResponse(
                sc.getStudent().getId(),
                sc.getStudent().getName(),
                status
            ));
        }
        return result;
    }

    /**
     * classRoomId+date로 학년도별 StudentClass 조회 → 각 학생의 출석 상태 반환
     */
    public List<StudentAttendanceResponse> findStudentAttendancesByClassAndDate(
        Long classRoomId,
        LocalDate date
    ) {
        // 1) date 기준 학년도 계산 (3월 시작 가정)
        int schoolYear = date.getMonthValue() >= 3
            ? date.getYear()
            : date.getYear() - 1;

        // 2) 해당 학년도, 해당 반에 속한 studentClass 모두 조회
        List<StudentClass> scList =
            studentClassRepository.findAllByClassRoom_IdAndSchoolYear(classRoomId, schoolYear);

        // 3) 각 studentClass별로 attendance 조회 후 DTO 변환
        return scList.stream()
            .map(sc -> {
                Optional<Attendance> opt =
                    attendanceRepository.findByStudentClassIdAndDate(sc.getId(), date);
                String status = opt
                    .map(a -> a.getStatus().name())
                    .orElse("UNCHECKED");
                return new StudentAttendanceResponse(
                    sc.getStudent().getId(),
                    sc.getStudent().getName(),
                    status
                );
            })
            .collect(Collectors.toList());
    }

    public List<SundayAttendanceSummaryResponse> getSundayAttendanceSummary() {
        List<LocalDate> sundays = attendanceRepository.findDistinctSundays();
        long totalStudentCount = studentRepository.count();

        return sundays.stream()
            .map(sunday -> {
                long attendedCount = attendanceRepository.countByDateAndStatus(sunday, AttendanceStatus.ATTEND);
                attendedCount += attendanceRepository.countByDateAndStatus(sunday, AttendanceStatus.LATE);
                return new SundayAttendanceSummaryResponse(sunday, attendedCount, totalStudentCount);
            })
            .collect(Collectors.toList());
    }

    public List<ClassSundayAttendanceResponse> getSundayAttendanceSummaryForClass(Long classRoomId) {
        // 1. Get all Sundays from attendance data
        List<LocalDate> sundays = attendanceRepository.findDistinctSundays();

        return sundays.stream()
            .map(sunday -> {
                // 2. For each Sunday, calculate schoolYear
                int schoolYear = sunday.getMonthValue() >= 3 ? sunday.getYear() : sunday.getYear() - 1;

                // 3. Get total students in the class for that school year
                long totalCount = studentClassRepository.countByClassRoomIdAndSchoolYear(classRoomId, schoolYear);

                // 4. Get attended students
                long attendedCount = attendanceRepository.countByClassRoomIdAndDateAndStatusIn(classRoomId, sunday);

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

        // 3. 각 ClassRoom에 대해 ClassAttendanceResponse를 생성합니다.
        return studentClassesByRoom.entrySet().stream()
            .map(entry -> {
                ClassRoom classRoom = entry.getKey();
                List<StudentClass> studentsInClass = entry.getValue();

                // 4. 해당 ClassRoom의 선생님 이름을 찾습니다.
                String teacherName = teacherClassRepository.findByClassRoomIdAndSchoolYear(classRoom.getId(), schoolYear)
                    .map(teacherClass -> teacherClass.getTeacher().getName())
                    .orElse("담당 선생님 없음"); // 담당 선생님이 없을 경우 기본값

                // 5. 학생별 출석 상태를 가져옵니다.
                List<StudentAttendanceStatusDto> studentAttendanceStatuses = studentsInClass.stream()
                    .map(sc -> {
                        Optional<Attendance> attendanceOpt = attendanceRepository.findByStudentClassIdAndDate(sc.getId(), date);
                        AttendanceStatus status = attendanceOpt
                            .map(Attendance::getStatus)
                            .orElse(AttendanceStatus.UNCHECKED); // 출석 기록이 없으면 UNCHECKED

                        return new StudentAttendanceStatusDto(
                            sc.getId(),
                            sc.getStudent().getName(),
                            status
                        );
                    })
                    // 학생 이름순으로 정렬
                    .sorted(Comparator.comparing(StudentAttendanceStatusDto::getStudentName))
                    .collect(Collectors.toList());

                // 6. ClassAttendanceResponse 객체를 생성합니다.
                return new ClassAttendanceResponse(
                    classRoom.getId(),
                    classRoom.getName(),
                    teacherName,
                    studentAttendanceStatuses
                );
            })
            // 반 이름 또는 학년-반 번호 순으로 정렬
            .sorted(Comparator
                .comparing(ClassAttendanceResponse::getClassName)) // Assuming classRoom.getName() provides a sortable order
            .collect(Collectors.toList());
    }
}

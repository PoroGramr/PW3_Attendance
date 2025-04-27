package com.jspark.pw3_attendant.service;

import com.jspark.pw3_attendant.domain.Attendance;
import com.jspark.pw3_attendant.domain.AttendanceStatus;
import com.jspark.pw3_attendant.domain.StudentClass;
import com.jspark.pw3_attendant.repository.AttendanceRepository;
import com.jspark.pw3_attendant.repository.StudentClassRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentClassRepository studentClassRepository;

    /**
     * 출석 체크
     */
    @Transactional
    public void saveAttendance(Long studentId, Long classRoomId, Integer schoolYear, LocalDate date, AttendanceStatus status) {
        // 1. 해당 학생-반 소속 찾기 (연도 기준)
        StudentClass studentClass = studentClassRepository.findByStudentIdAndSchoolYear(studentId, schoolYear)
            .orElseThrow(() -> new IllegalArgumentException("학생 소속 정보를 찾을 수 없습니다."));

        // 2. 해당 날짜에 출석 기록이 이미 있는지 확인
        Attendance attendance = attendanceRepository.findByStudentClassIdAndDate(studentClass.getId(), date)
            .orElse(null);

        if (attendance == null) {
            // 없다면 새로 생성
            Attendance newAttendance = new Attendance();
            newAttendance.setStudentClass(studentClass);
            newAttendance.setDate(date);
            newAttendance.setStatus(status);

            attendanceRepository.save(newAttendance);
        } else {
            // 있으면 업데이트
            attendance.setStatus(status);
        }
    }

    @Transactional(readOnly = true)
    public List<Attendance> findAttendancesByClassRoomAndDate(Long classRoomId, Integer schoolYear, LocalDate date) {
        List<StudentClass> studentClasses = studentClassRepository.findAllByClassRoomIdAndSchoolYear(classRoomId, schoolYear);
        List<Long> studentClassIds = studentClasses.stream()
            .map(StudentClass::getId)
            .collect(Collectors.toList());
        return attendanceRepository.findAllByStudentClassIdInAndDate(studentClassIds, date);
    }

}

package com.jspark.pw3_attendant.service;

import com.jspark.pw3_attendant.domain.Attendance;
import com.jspark.pw3_attendant.domain.AttendanceStatus;
import com.jspark.pw3_attendant.domain.StudentClass;
import com.jspark.pw3_attendant.repository.AttendanceRepository;
import com.jspark.pw3_attendant.repository.StudentClassRepository;
import com.jspark.pw3_attendant.service.dto.StudentAttendanceResponse;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentClassRepository studentClassRepository;

    @Transactional
    public void saveAttendance(Long studentClassId, LocalDate date, AttendanceStatus status) {
        StudentClass studentClass = studentClassRepository.findById(studentClassId)
            .orElseThrow(() -> new IllegalArgumentException("학생-반 매핑을 찾을 수 없습니다."));

        Attendance attendance = new Attendance();
        attendance.setStudentClass(studentClass);
        attendance.setDate(date);
        attendance.setStatus(status);

        attendanceRepository.save(attendance);
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

}

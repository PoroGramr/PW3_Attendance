package com.jspark.pw3_attendant.service.Attendance;


import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;

import com.jspark.pw3_attendant.repository.Attendance.AttendanceRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.service.Attendance.dto.StudentAttendanceResponse;
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


}

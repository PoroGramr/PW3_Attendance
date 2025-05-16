package com.jspark.pw3_attendant.service.Attendance;

import com.jspark.pw3_attendant.domain.Attendance.AttendanceTeacher;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.repository.Attendance.AttendanceTeacherRepository;
import com.jspark.pw3_attendant.repository.Teacher.TeacherRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AttendanceTeacherService {

    private final AttendanceTeacherRepository attendanceTeacherRepository;
    private final TeacherRepository teacherRepository;

    public AttendanceTeacherService(AttendanceTeacherRepository attendanceTeacherRepository,
        TeacherRepository teacherRepository) {
        this.attendanceTeacherRepository = attendanceTeacherRepository;
        this.teacherRepository = teacherRepository;
    }

    // 선생님 본인 출석 체크
    public void markTeacherAttendance(Long teacherId, AttendanceTeacher.AttendanceStatus status, LocalDate date) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("선생님을 찾을 수 없습니다."));

        // 선생님과 날짜에 해당하는 출석 정보를 조회
        AttendanceTeacher attendanceTeacher = attendanceTeacherRepository.findByTeacherAndDate(teacher, date)
            .orElse(new AttendanceTeacher()); // 없으면 새로 생성

        // 출석 정보 업데이트
        attendanceTeacher.setTeacher(teacher);
        attendanceTeacher.setDate(date);
        attendanceTeacher.setStatus(status);

        // 출석 정보 저장 (새로 추가하거나 기존 값을 업데이트)
        attendanceTeacherRepository.save(attendanceTeacher);
    }

    // 선생님 본인 출석 상태 조회
    public AttendanceTeacher getTeacherAttendanceStatus(Long teacherId, LocalDate date) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("선생님을 찾을 수 없습니다."));

        return attendanceTeacherRepository.findByTeacherAndDate(teacher, date)
            .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 출석 기록이 없습니다."));
    }
    public List<AttendanceTeacher> getAllTeachersAttendanceByDate(LocalDate date) {
        return attendanceTeacherRepository.findByDate(date);
    }

}
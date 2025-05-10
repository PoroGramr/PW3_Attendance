package com.jspark.pw3_attendant.service.Attendance;

import com.jspark.pw3_attendant.domain.Attendance.AttendanceTeacher;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.repository.Attendance.AttendanceTeacherRepository;
import com.jspark.pw3_attendant.repository.Teacher.TeacherRepository;
import java.time.LocalDate;
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
    public void markTeacherAttendance(Long teacherId, Boolean isPresent, LocalDate date) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("선생님을 찾을 수 없습니다."));

        AttendanceTeacher attendanceTeacher = new AttendanceTeacher();
        attendanceTeacher.setTeacher(teacher);
        attendanceTeacher.setDate(date);
        attendanceTeacher.setStatus(isPresent ? AttendanceTeacher.AttendanceStatus.ATTEND : AttendanceTeacher.AttendanceStatus.ABSENT);

        attendanceTeacherRepository.save(attendanceTeacher);
    }
}

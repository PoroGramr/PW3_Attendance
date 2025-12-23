package com.jspark.pw3_attendant.repository.Attendance;

import com.jspark.pw3_attendant.domain.Attendance.AttendanceTeacher;
import com.jspark.pw3_attendant.domain.Attendance.AttendanceTeacher.AttendanceStatus;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceTeacherRepository extends JpaRepository<AttendanceTeacher, Long> {

    // 선생님과 날짜에 해당하는 출석 기록 조회
    Optional<AttendanceTeacher> findByTeacherAndDate(Teacher teacher, LocalDate date);

    // 선생님과 날짜에 해당하는 출석 기록이 존재하는지 확인
    boolean existsByTeacherAndDate(Teacher teacher, LocalDate date);

    List<AttendanceTeacher> findByDate(LocalDate date);

    long countByDateAndStatusIn(LocalDate date, List<AttendanceStatus> statuses);
}

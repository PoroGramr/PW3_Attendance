package com.jspark.pw3_attendant.repository.Attendance;

import com.jspark.pw3_attendant.domain.Attendance.AttendanceTeacher;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceTeacherRepository extends JpaRepository<AttendanceTeacher, Long> {

    // 특정 선생님과 날짜에 해당하는 출석 정보 조회
    Optional<AttendanceTeacher> findByTeacherAndDate(Teacher teacher, LocalDate date);

    // 특정 선생님에 대해 특정 날짜의 출석 정보가 존재하는지 확인하는 메서드
    boolean existsByTeacherAndDate(Teacher teacher, LocalDate date);
}
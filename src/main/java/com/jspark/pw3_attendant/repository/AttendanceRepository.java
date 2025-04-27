package com.jspark.pw3_attendant.repository;

import com.jspark.pw3_attendant.domain.Attendance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 학생반 + 날짜 기준으로 출석 기록 찾기
    Optional<Attendance> findByStudentClassIdAndDate(Long studentClassId, LocalDate date);

    // 여러 학생반 ID 목록 + 날짜로 출석 기록 일괄 조회
    List<Attendance> findAllByStudentClassIdInAndDate(List<Long> studentClassIds, LocalDate date);
}

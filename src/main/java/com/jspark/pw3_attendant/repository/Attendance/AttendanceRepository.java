package com.jspark.pw3_attendant.repository.Attendance;



import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 특정 학생-반 매핑(studentClassId)로 모든 출석 기록 조회
    List<Attendance> findAllByStudentClassId(Long studentClassId);

    // 특정 학생-반 매핑(studentClassId) + 특정 날짜(date) 출석 기록 조회
    List<Attendance> findAllByStudentClassIdAndDate(Long studentClassId, LocalDate date);

    Optional<Attendance> findByStudentClassIdAndDate(Long studentClassId, LocalDate date); // 🔥 딱 1개만 조회용

    // 특정 반(classRoomId) + 특정 연도(schoolYear) + 날짜(date)로 출석 조회 (확장용, 선택)
    // -> 필요 시 Query 추가 가능

    @Query("SELECT DISTINCT a.date FROM Attendance a WHERE DAYOFWEEK(a.date) = 1 ORDER BY a.date DESC")
    List<LocalDate> findDistinctSundays();

    long countByDateAndStatus(LocalDate date, AttendanceStatus status);
}

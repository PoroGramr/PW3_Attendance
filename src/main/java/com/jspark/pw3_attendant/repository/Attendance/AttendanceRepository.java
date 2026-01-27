package com.jspark.pw3_attendant.repository.Attendance;

import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

        long countByDateAndStatusIn(LocalDate date, List<AttendanceStatus> statuses);

        List<Attendance> findByDateAndStatusIn(LocalDate date, List<AttendanceStatus> statuses);

        @Query("SELECT COUNT(a) FROM Attendance a " +
                        "JOIN a.studentClass sc " +
                        "WHERE sc.classRoom.id = :classRoomId " +
                        "AND a.date = :date " +
                        "AND a.status IN ('ATTEND', 'LATE')")
        long countByClassRoomIdAndDateAndStatusIn(@Param("classRoomId") Long classRoomId,
                        @Param("date") LocalDate date);

        List<Attendance> findAllByStudentClassInAndDate(List<StudentClass> studentClasses, LocalDate date);

        List<Attendance> findByDateBetweenAndStatusIn(LocalDate startDate, LocalDate endDate,
                        List<AttendanceStatus> statuses);

        @Query("SELECT a FROM Attendance a " +
                        "JOIN FETCH a.studentClass sc " +
                        "JOIN FETCH sc.student " +
                        "JOIN FETCH sc.classRoom " +
                        "WHERE a.date BETWEEN :startDate AND :endDate " +
                        "AND a.status IN :statuses")
        List<Attendance> findByDateBetweenAndStatusInWithStudentClass(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("statuses") List<AttendanceStatus> statuses);

        // 일요일만 필터링하는 버전 (DAYOFWEEK = 1 is Sunday in MySQL)
        @Query("SELECT a FROM Attendance a " +
                        "JOIN FETCH a.studentClass sc " +
                        "JOIN FETCH sc.student " +
                        "JOIN FETCH sc.classRoom " +
                        "WHERE a.date BETWEEN :startDate AND :endDate " +
                        "AND DAYOFWEEK(a.date) = 1 " +
                        "AND a.status IN :statuses")
        List<Attendance> findSundayAttendanceByDateBetweenAndStatusIn(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("statuses") List<AttendanceStatus> statuses);

        @Query("SELECT COUNT(a) FROM Attendance a " +
                        "JOIN a.studentClass sc " +
                        "JOIN sc.classRoom cr " +
                        "WHERE cr.schoolType = :schoolType " +
                        "AND cr.grade = :grade " +
                        "AND a.date = :date " +
                        "AND sc.schoolYear = :schoolYear " +
                        "AND a.status IN ('ATTEND', 'LATE')")
        long countByGradeAndDateAndStatusIn(
                        @Param("schoolType") ClassRoom.SchoolType schoolType,
                        @Param("grade") Integer grade,
                        @Param("date") LocalDate date,
                        @Param("schoolYear") Integer schoolYear);
}

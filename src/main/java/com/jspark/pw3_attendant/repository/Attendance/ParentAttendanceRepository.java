package com.jspark.pw3_attendant.repository.Attendance;

import com.jspark.pw3_attendant.domain.Attendance.ParentAttendance;
import com.jspark.pw3_attendant.domain.Student.Student;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentAttendanceRepository extends JpaRepository<ParentAttendance, Long> {

    Optional<ParentAttendance> findByStudentAndDate(Student student, LocalDate date);

    List<ParentAttendance> findByDate(LocalDate date);
}

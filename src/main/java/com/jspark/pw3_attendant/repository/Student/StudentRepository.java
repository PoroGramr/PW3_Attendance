package com.jspark.pw3_attendant.repository.Student;

import com.jspark.pw3_attendant.domain.Student.Student;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findAllByStudentClassesIsEmpty();

    @Query("SELECT s FROM Student s WHERE FUNCTION('YEAR', s.createdAt) = :year")
    List<Student> findAllByYear(@Param("year") int year);

    List<Student> findAllByCreatedAtAfter(LocalDateTime dateTime);

    @Query("SELECT s FROM Student s WHERE MONTH(s.birth) = :month AND s.deletedAt IS NULL AND NOT (MONTH(s.birth) = 1 AND DAY(s.birth) = 1) ORDER BY DAY(s.birth)")
    List<Student> findByBirthMonth(@Param("month") int month);

    // 재학생만 조회 (졸업하지 않은 학생)
    List<Student> findAllByIsGraduatedFalse();

    // 졸업생만 조회
    List<Student> findAllByIsGraduatedTrue();
}

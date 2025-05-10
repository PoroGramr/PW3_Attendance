package com.jspark.pw3_attendant.repository.Student;


import com.jspark.pw3_attendant.domain.Student.Student;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findAllByStudentClassesIsEmpty();
}

package com.jspark.pw3_attendant.repository.Teacher;


import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

}

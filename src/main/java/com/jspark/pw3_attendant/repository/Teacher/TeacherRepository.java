package com.jspark.pw3_attendant.repository.Teacher;


import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByIdAndDeletedAtIsNull(Long id);

    List<Teacher> findAllByDeletedAtIsNull();
}

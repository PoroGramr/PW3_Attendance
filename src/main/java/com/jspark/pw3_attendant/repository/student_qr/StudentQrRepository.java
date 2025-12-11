package com.jspark.pw3_attendant.repository.student_qr;

import com.jspark.pw3_attendant.domain.student_qr.StudentQr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentQrRepository extends JpaRepository<StudentQr, Long> {

    Optional<StudentQr> findByQrSecret(String qrSecret);

    Optional<StudentQr> findByStudentId(Long studentId);

    boolean existsByQrSecret(String qrSecret);
}

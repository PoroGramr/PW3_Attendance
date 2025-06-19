package com.jspark.pw3_attendant.repository.Pray;

import com.jspark.pw3_attendant.domain.Pray.Pray;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrayRepository extends JpaRepository<Pray, Long> {

    Optional<Pray> findByDate(LocalDate date);
}

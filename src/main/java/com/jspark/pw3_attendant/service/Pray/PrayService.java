package com.jspark.pw3_attendant.service.Pray;

import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.Pray.Pray;
import com.jspark.pw3_attendant.repository.Pray.PrayRepository;
import java.time.LocalDate;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrayService {
    private final PrayRepository prayRepository;

    public Pray findByDate(LocalDate date) {
        return prayRepository.findByDate(date)
            .orElseThrow(() -> new IllegalArgumentException("날짜를 찾을 수 없습니다."));
    }
}

package com.jspark.pw3_attendant.service.auth;

import com.jspark.pw3_attendant.repository.admin.AdminRefreshTokenRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupService {

    private final AdminRefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteAllByExpiresAtBefore(LocalDateTime.now(ZoneOffset.UTC));
    }
}

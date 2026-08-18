package com.jspark.pw3_attendant.repository.admin;

import com.jspark.pw3_attendant.domain.admin.AdminRefreshToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("update AdminRefreshToken t set t.revokedAt = :now "
            + "where t.admin.id = :adminId and t.revokedAt is null")
    int revokeAllByAdminId(@Param("adminId") Long adminId, @Param("now") LocalDateTime now);

    void deleteAllByExpiresAtBefore(LocalDateTime threshold);
}

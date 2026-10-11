package com.jspark.pw3_attendant.repository.admin;

import com.jspark.pw3_attendant.domain.admin.AdminAccountRecovery;
import com.jspark.pw3_attendant.domain.admin.RecoveryPurpose;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface AdminAccountRecoveryRepository extends JpaRepository<AdminAccountRecovery, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AdminAccountRecovery r join fetch r.admin where r.publicId = :publicId")
    Optional<AdminAccountRecovery> findByPublicIdForUpdate(@Param("publicId") String publicId);

    boolean existsByAdminIdAndPurposeAndCreatedAtAfter(
            Long adminId,
            RecoveryPurpose purpose,
            LocalDateTime threshold);

    Optional<AdminAccountRecovery> findFirstByAdminIdAndPurposeOrderByCreatedAtDesc(
            Long adminId,
            RecoveryPurpose purpose);

    void deleteAllByExpiresAtBefore(LocalDateTime threshold);

    @Modifying
    @Query("update AdminAccountRecovery r set r.consumedAt = :now where r.admin.id = :adminId "
            + "and r.purpose = com.jspark.pw3_attendant.domain.admin.RecoveryPurpose.PASSWORD_RESET "
            + "and r.consumedAt is null")
    int consumeUnusedPasswordResets(@Param("adminId") Long adminId, @Param("now") LocalDateTime now);
}

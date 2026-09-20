package com.jspark.pw3_attendant.domain.admin;

import com.jspark.pw3_attendant.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "admin_account_recovery")
public class AdminAccountRecovery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private AdminAccount admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecoveryPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecoveryChannel channel;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    public static AdminAccountRecovery passwordReset(
            AdminAccount admin,
            RecoveryChannel channel,
            String codeHash,
            LocalDateTime expiresAt) {
        AdminAccountRecovery recovery = new AdminAccountRecovery();
        recovery.publicId = UUID.randomUUID().toString();
        recovery.admin = admin;
        recovery.purpose = RecoveryPurpose.PASSWORD_RESET;
        recovery.channel = channel;
        recovery.codeHash = codeHash;
        recovery.expiresAt = expiresAt;
        return recovery;
    }

    public static AdminAccountRecovery usernameReminder(
            AdminAccount admin,
            RecoveryChannel channel,
            String auditHash,
            LocalDateTime now) {
        AdminAccountRecovery recovery = new AdminAccountRecovery();
        recovery.publicId = UUID.randomUUID().toString();
        recovery.admin = admin;
        recovery.purpose = RecoveryPurpose.USERNAME_REMINDER;
        recovery.channel = channel;
        recovery.codeHash = auditHash;
        recovery.expiresAt = now;
        recovery.consumedAt = now;
        return recovery;
    }

    public boolean isUsable(LocalDateTime now, int maxAttempts) {
        return consumedAt == null && expiresAt.isAfter(now) && failedAttempts < maxAttempts;
    }

    public void registerFailure(int maxAttempts, LocalDateTime now) {
        failedAttempts++;
        if (failedAttempts >= maxAttempts) {
            consumedAt = now;
        }
    }

    public void consume(LocalDateTime now) {
        consumedAt = now;
    }
}

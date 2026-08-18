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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "admin_account")
public class AdminAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AdminAccount approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_reason", length = 500)
    private String rejectedReason;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public static AdminAccount pending(
            String username, String passwordHash, String name, String email, String phone) {
        AdminAccount account = new AdminAccount();
        account.username = username;
        account.passwordHash = passwordHash;
        account.name = name;
        account.email = email;
        account.phone = phone;
        account.role = AdminRole.ADMIN;
        account.approvalStatus = ApprovalStatus.PENDING;
        return account;
    }

    public static AdminAccount superAdmin(
            String username, String passwordHash, String name, String email, String phone) {
        AdminAccount account = new AdminAccount();
        account.username = username;
        account.passwordHash = passwordHash;
        account.name = name;
        account.email = email;
        account.phone = phone;
        account.role = AdminRole.SUPER_ADMIN;
        account.approvalStatus = ApprovalStatus.APPROVED;
        account.approvedAt = LocalDateTime.now();
        return account;
    }

    public void approve(AdminAccount superAdmin) {
        requireSuperAdmin(superAdmin);
        requirePending();
        approvalStatus = ApprovalStatus.APPROVED;
        approvedBy = superAdmin;
        approvedAt = LocalDateTime.now();
        rejectedReason = null;
    }

    public void reject(AdminAccount superAdmin, String reason) {
        requireSuperAdmin(superAdmin);
        requirePending();
        approvalStatus = ApprovalStatus.REJECTED;
        approvedBy = superAdmin;
        approvedAt = LocalDateTime.now();
        rejectedReason = reason;
    }

    public void resubmit(String name, String email, String phone) {
        if (approvalStatus != ApprovalStatus.REJECTED) {
            throw new IllegalStateException("거절된 계정만 재신청할 수 있습니다.");
        }
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.approvalStatus = ApprovalStatus.PENDING;
        this.approvedBy = null;
        this.approvedAt = null;
        this.rejectedReason = null;
    }

    public void recordLogin() {
        lastLoginAt = LocalDateTime.now();
    }

    private void requirePending() {
        if (approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("승인 대기 중인 계정만 처리할 수 있습니다.");
        }
    }

    private static void requireSuperAdmin(AdminAccount account) {
        if (account == null || account.role != AdminRole.SUPER_ADMIN
                || account.approvalStatus != ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("승인된 슈퍼어드민만 처리할 수 있습니다.");
        }
    }
}

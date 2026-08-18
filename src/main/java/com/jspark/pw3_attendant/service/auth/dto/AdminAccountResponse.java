package com.jspark.pw3_attendant.service.auth.dto;

import com.jspark.pw3_attendant.domain.admin.AdminAccount;
import com.jspark.pw3_attendant.domain.admin.AdminRole;
import com.jspark.pw3_attendant.domain.admin.ApprovalStatus;
import java.time.LocalDateTime;

public record AdminAccountResponse(
        Long id,
        String username,
        String name,
        String email,
        String phone,
        AdminRole role,
        ApprovalStatus approvalStatus,
        Long approvedById,
        LocalDateTime approvedAt,
        String rejectedReason,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static AdminAccountResponse from(AdminAccount account) {
        return new AdminAccountResponse(
                account.getId(),
                account.getUsername(),
                account.getName(),
                account.getEmail(),
                account.getPhone(),
                account.getRole(),
                account.getApprovalStatus(),
                account.getApprovedBy() == null ? null : account.getApprovedBy().getId(),
                account.getApprovedAt(),
                account.getRejectedReason(),
                account.getLastLoginAt(),
                account.getCreatedAt()
        );
    }
}

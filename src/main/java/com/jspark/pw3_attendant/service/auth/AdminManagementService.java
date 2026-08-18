package com.jspark.pw3_attendant.service.auth;

import com.jspark.pw3_attendant.common.exception.ApiException;
import com.jspark.pw3_attendant.domain.admin.AdminAccount;
import com.jspark.pw3_attendant.domain.admin.AdminRole;
import com.jspark.pw3_attendant.domain.admin.ApprovalStatus;
import com.jspark.pw3_attendant.repository.admin.AdminAccountRepository;
import com.jspark.pw3_attendant.service.auth.dto.AdminAccountResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final AdminAccountRepository adminAccountRepository;

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> findByStatus(ApprovalStatus status) {
        return adminAccountRepository.findAllByApprovalStatusOrderByCreatedAtAsc(status).stream()
                .map(AdminAccountResponse::from)
                .toList();
    }

    @Transactional
    public AdminAccountResponse approve(Long targetId, Long superAdminId) {
        AdminAccount superAdmin = requireSuperAdmin(superAdminId);
        AdminAccount target = requireTargetAdmin(targetId);
        ensurePending(target);
        target.approve(superAdmin);
        return AdminAccountResponse.from(target);
    }

    @Transactional
    public AdminAccountResponse reject(Long targetId, Long superAdminId, String reason) {
        AdminAccount superAdmin = requireSuperAdmin(superAdminId);
        AdminAccount target = requireTargetAdmin(targetId);
        ensurePending(target);
        target.reject(superAdmin, reason.trim());
        return AdminAccountResponse.from(target);
    }

    private AdminAccount requireSuperAdmin(Long id) {
        AdminAccount account = adminAccountRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        if (account.getRole() != AdminRole.SUPER_ADMIN
                || account.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SUPER_ADMIN_REQUIRED", "승인된 슈퍼어드민만 처리할 수 있습니다.");
        }
        return account;
    }

    private AdminAccount requireTargetAdmin(Long id) {
        AdminAccount account = adminAccountRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        if (account.getRole() != AdminRole.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_APPROVAL_TARGET", "일반 관리자 계정만 승인 처리할 수 있습니다.");
        }
        return account;
    }

    private void ensurePending(AdminAccount account) {
        if (account.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN_NOT_PENDING", "승인 대기 중인 계정만 처리할 수 있습니다.");
        }
    }

    private ApiException notFound(Long id) {
        return new ApiException(HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND", "관리자 계정을 찾을 수 없습니다: " + id);
    }
}

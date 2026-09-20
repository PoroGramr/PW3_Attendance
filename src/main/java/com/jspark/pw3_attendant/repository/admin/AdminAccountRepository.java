package com.jspark.pw3_attendant.repository.admin;

import com.jspark.pw3_attendant.domain.admin.AdminAccount;
import com.jspark.pw3_attendant.domain.admin.ApprovalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {

    Optional<AdminAccount> findByUsername(String username);

    Optional<AdminAccount> findByEmail(String email);

    List<AdminAccount> findAllByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<AdminAccount> findAllByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus approvalStatus);
}

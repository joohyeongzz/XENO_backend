package com.daewon.xeno_backend.repository.auth;

import com.daewon.xeno_backend.domain.auth.BrandApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandApprovalRepository extends JpaRepository<BrandApproval, Long> {

    Optional<BrandApproval> findByBrandNameAndEmailAndStatus(String brandName, String email, String status);
}

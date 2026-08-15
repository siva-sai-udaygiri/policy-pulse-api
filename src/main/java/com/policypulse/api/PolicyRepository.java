package com.policypulse.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Page<Policy> findByStatusIgnoreCase(String status, Pageable pageable);
    boolean existsByPolicyNumber(String policyNumber);
}

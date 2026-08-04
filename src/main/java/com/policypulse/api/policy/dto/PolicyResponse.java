package com.policypulse.api.policy.dto;

import com.policypulse.api.policy.domain.PolicyStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PolicyResponse(
        Long id,
        String policyNumber,
        String holderName,
        PolicyStatus status,
        BigDecimal premium,
        boolean hasDocument,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
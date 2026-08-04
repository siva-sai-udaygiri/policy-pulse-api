package com.policypulse.api.policy.dto;

import com.policypulse.api.policy.domain.PolicyStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePolicyRequest(
        @NotBlank
        @Size(max = 50)
        String policyNumber,

        @NotBlank
        @Size(max = 120)
        String holderName,

        @NotNull
        PolicyStatus status,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal premium
) {
}
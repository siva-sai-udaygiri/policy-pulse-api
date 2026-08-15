package com.policypulse.api.policy.dto;

import com.policypulse.api.policy.domain.PolicyStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePolicyRequest(
        @NotBlank(message = "Policy number is required")
        @Size(max = 50, message = "Policy number must be at most 50 characters")
        String policyNumber,

        @NotBlank(message = "Holder name is required")
        @Size(max = 120, message = "Holder name must be at most 120 characters")
        String holderName,

        @NotNull(message = "Policy status is required")
        PolicyStatus status,

        @NotNull(message = "Premium is required")
        @DecimalMin(value = "0.0", message = "Premium must be greater than or equal to 0")
        BigDecimal premium
) {
}
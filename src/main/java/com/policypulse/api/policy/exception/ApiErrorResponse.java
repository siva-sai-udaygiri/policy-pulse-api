package com.policypulse.api.policy.exception;

public record ApiErrorResponse(
        int status,
        String message
) {
}
package com.policypulse.api.policy.exception;

import java.util.Map;

public record ApiValidationErrorResponse(
        int status,
        String message,
        Map<String, String> fieldErrors
) {
}
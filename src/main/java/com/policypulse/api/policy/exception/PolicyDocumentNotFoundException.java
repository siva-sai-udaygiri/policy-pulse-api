package com.policypulse.api.policy.exception;

public class PolicyDocumentNotFoundException extends RuntimeException {

    public PolicyDocumentNotFoundException(Long id) {
        super("No document found for policy: " + id);
    }
}
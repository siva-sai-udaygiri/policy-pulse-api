package com.policypulse.api.policy.exception;

public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(Long id) {
        super("Policy not found: " + id);
    }
}
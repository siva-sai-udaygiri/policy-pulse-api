package com.policypulse.api.policy.exception;

public class DuplicatePolicyException extends RuntimeException {

    public DuplicatePolicyException(String policyNumber) {
        super("Policy already exists with policy number: " + policyNumber);
    }
}
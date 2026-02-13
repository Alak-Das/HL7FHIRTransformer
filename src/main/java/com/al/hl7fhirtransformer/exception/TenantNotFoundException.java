package com.al.hl7fhirtransformer.exception;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String message) {
        super(message);
    }
}

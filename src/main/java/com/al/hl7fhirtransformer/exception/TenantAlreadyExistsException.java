package com.al.hl7fhirtransformer.exception;

public class TenantAlreadyExistsException extends RuntimeException {
    public TenantAlreadyExistsException(String message) {
        super(message);
    }
}

package com.al.hl7fhirtransformer.exception;

import java.util.List;

/**
 * Exception thrown when FHIR resource validation fails.
 * Contains detailed validation error messages for client debugging.
 */
public class FhirValidationException extends RuntimeException {

    private final List<ValidationError> validationErrors;

    public FhirValidationException(String message, List<ValidationError> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Represents a single validation error with location and severity
     */
    public static class ValidationError {
        private final String severity;
        private final String location;
        private final String message;

        public ValidationError(String severity, String location, String message) {
            this.severity = severity;
            this.location = location;
            this.message = message;
        }

        public String getSeverity() {
            return severity;
        }

        public String getLocation() {
            return location;
        }

        public String getMessage() {
            return message;
        }
    }
}

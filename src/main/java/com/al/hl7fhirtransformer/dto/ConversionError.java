package com.al.hl7fhirtransformer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an error that occurred during HL7 to FHIR conversion.
 */
public class ConversionError {

    /**
     * The segment where the error occurred (e.g., "PID", "OBX", "PV1")
     */
    private String segment;

    /**
     * The segment index (for repeating segments like OBX)
     */
    private int segmentIndex;

    /**
     * The field number where the error occurred (if applicable)
     */
    private String field;

    /**
     * Error code for programmatic handling
     */
    private String errorCode;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Severity: ERROR, WARNING, INFORMATION
     */
    private Severity severity;

    /**
     * The original exception class name (for debugging)
     */
    private String exceptionType;

    public ConversionError() {
    }

    public ConversionError(String segment, int segmentIndex, String field, String errorCode, String message,
            Severity severity, String exceptionType) {
        this.segment = segment;
        this.segmentIndex = segmentIndex;
        this.field = field;
        this.errorCode = errorCode;
        this.message = message;
        this.severity = severity;
        this.exceptionType = exceptionType;
    }

    public enum Severity {
        ERROR,
        WARNING,
        INFORMATION
    }

    public static ConversionError segmentError(String segment, int index, String message) {
        return new Builder()
                .segment(segment)
                .segmentIndex(index)
                .message(message)
                .severity(Severity.ERROR)
                .errorCode("SEGMENT_ERROR")
                .build();
    }

    public static ConversionError fieldError(String segment, String field, String message) {
        return new Builder()
                .segment(segment)
                .field(field)
                .message(message)
                .severity(Severity.ERROR)
                .errorCode("FIELD_ERROR")
                .build();
    }

    public static ConversionError warning(String segment, String message) {
        return new Builder()
                .segment(segment)
                .message(message)
                .severity(Severity.WARNING)
                .errorCode("WARNING")
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public void setSegmentIndex(int segmentIndex) {
        this.segmentIndex = segmentIndex;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public static class Builder {
        private String segment;
        private int segmentIndex;
        private String field;
        private String errorCode;
        private String message;
        private Severity severity;
        private String exceptionType;

        public Builder segment(String segment) {
            this.segment = segment;
            return this;
        }

        public Builder segmentIndex(int segmentIndex) {
            this.segmentIndex = segmentIndex;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder exceptionType(String exceptionType) {
            this.exceptionType = exceptionType;
            return this;
        }

        public ConversionError build() {
            return new ConversionError(segment, segmentIndex, field, errorCode, message, severity, exceptionType);
        }
    }
}

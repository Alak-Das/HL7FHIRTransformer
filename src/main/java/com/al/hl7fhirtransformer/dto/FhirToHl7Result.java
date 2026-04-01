package com.al.hl7fhirtransformer.dto;


import java.util.ArrayList;
import java.util.List;

/**
 * Result wrapper for FHIR to HL7 conversion.
 * Contains the converted message along with any errors/warnings.
 */
public class FhirToHl7Result {
    private String hl7Message;
    private String messageType;
    private List<ConversionError> errors = new ArrayList<>();
    private List<ConversionError> warnings = new ArrayList<>();
    private int successCount;
    private int failCount;

    public FhirToHl7Result() {
    }

    public FhirToHl7Result(String hl7Message, String messageType, List<ConversionError> errors,
            List<ConversionError> warnings, int successCount, int failCount) {
        this.hl7Message = hl7Message;
        this.messageType = messageType;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.successCount = successCount;
        this.failCount = failCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getHl7Message() {
        return hl7Message;
    }

    public void setHl7Message(String hl7Message) {
        this.hl7Message = hl7Message;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public List<ConversionError> getErrors() {
        return errors;
    }

    public void setErrors(List<ConversionError> errors) {
        this.errors = errors;
    }

    public List<ConversionError> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<ConversionError> warnings) {
        this.warnings = warnings;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean isPartialSuccess() {
        return hl7Message != null && hasErrors();
    }

    public boolean isFullSuccess() {
        return hl7Message != null && !hasErrors();
    }

    public static class Builder {
        private String hl7Message;
        private String messageType;
        private List<ConversionError> errors = new ArrayList<>();
        private List<ConversionError> warnings = new ArrayList<>();
        private int successCount;
        private int failCount;

        public Builder hl7Message(String hl7Message) {
            this.hl7Message = hl7Message;
            return this;
        }

        public Builder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder errors(List<ConversionError> errors) {
            this.errors = errors;
            return this;
        }

        public Builder warnings(List<ConversionError> warnings) {
            this.warnings = warnings;
            return this;
        }

        public Builder successCount(int successCount) {
            this.successCount = successCount;
            return this;
        }

        public Builder failCount(int failCount) {
            this.failCount = failCount;
            return this;
        }

        public FhirToHl7Result build() {
            return new FhirToHl7Result(hl7Message, messageType, errors, warnings, successCount, failCount);
        }
    }
}

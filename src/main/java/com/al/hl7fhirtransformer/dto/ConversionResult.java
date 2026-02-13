package com.al.hl7fhirtransformer.dto;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import java.util.List;
import java.util.ArrayList;

public class ConversionResult {

    /**
     * The converted FHIR Bundle (may be partial if errors occurred)
     */
    private Bundle bundle;

    /**
     * List of errors that occurred during conversion
     */
    private List<ConversionError> errors = new ArrayList<>();

    /**
     * List of warnings (non-fatal issues)
     */
    private List<ConversionError> warnings = new ArrayList<>();

    /**
     * FHIR OperationOutcome for detailed error reporting
     */
    private OperationOutcome operationOutcome;

    public ConversionResult() {
    }

    public ConversionResult(Bundle bundle, List<ConversionError> errors, List<ConversionError> warnings,
            OperationOutcome operationOutcome) {
        this.bundle = bundle;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.operationOutcome = operationOutcome;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
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

    public OperationOutcome getOperationOutcome() {
        return operationOutcome;
    }

    public void setOperationOutcome(OperationOutcome operationOutcome) {
        this.operationOutcome = operationOutcome;
    }

    public static class Builder {
        private Bundle bundle;
        private List<ConversionError> errors = new ArrayList<>();
        private List<ConversionError> warnings = new ArrayList<>();
        private OperationOutcome operationOutcome;

        public Builder bundle(Bundle bundle) {
            this.bundle = bundle;
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

        public Builder operationOutcome(OperationOutcome operationOutcome) {
            this.operationOutcome = operationOutcome;
            return this;
        }

        public ConversionResult build() {
            return new ConversionResult(bundle, errors, warnings, operationOutcome);
        }
    }

    /**
     * Whether any errors occurred
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Whether any warnings occurred
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    /**
     * Whether the conversion was a partial success (has bundle but also errors)
     */
    public boolean isPartialSuccess() {
        return bundle != null && hasErrors();
    }

    /**
     * Whether the conversion was fully successful (no errors)
     */
    public boolean isFullSuccess() {
        return bundle != null && !hasErrors();
    }

    /**
     * Add an error to the result
     */
    public void addError(ConversionError error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    /**
     * Add a warning to the result
     */
    public void addWarning(ConversionError warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }

    /**
     * Get total count of issues (errors + warnings)
     */
    public int getTotalIssueCount() {
        int count = 0;
        if (errors != null)
            count += errors.size();
        if (warnings != null)
            count += warnings.size();
        return count;
    }

    /**
     * Create a successful result with no errors
     */
    public static ConversionResult success(Bundle bundle) {
        return ConversionResult.builder()
                .bundle(bundle)
                .build();
    }

    /**
     * Create a failed result with an error message
     */
    public static ConversionResult failure(String errorMessage) {
        ConversionResult result = new ConversionResult();
        result.addError(ConversionError.builder()
                .message(errorMessage)
                .severity(ConversionError.Severity.ERROR)
                .errorCode("CONVERSION_FAILED")
                .build());
        return result;
    }
}

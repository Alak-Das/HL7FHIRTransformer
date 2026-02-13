package com.al.hl7fhirtransformer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for HL7 parsing and FHIR conversion behavior.
 */
@Configuration
@ConfigurationProperties(prefix = "app.parsing")
public class ParsingConfiguration {

    /**
     * Parsing strictness level.
     * STRICT: Fail on any parsing error
     * LENIENT: Continue on non-critical errors, collect warnings
     * PERMISSIVE: Best-effort parsing, ignore most errors
     */
    private StrictnessLevel strictness = StrictnessLevel.LENIENT;

    /**
     * Whether to continue processing other segments when one fails.
     * Only applicable when strictness is not STRICT.
     */
    private boolean continueOnError = true;

    /**
     * Whether to validate output FHIR resources.
     */
    private boolean validationEnabled = true;

    /**
     * Whether to fail the conversion if FHIR validation produces warnings.
     */
    private boolean failOnValidationWarning = false;

    /**
     * Whether to include OperationOutcome in the response bundle.
     */
    private boolean includeOperationOutcome = true;

    /**
     * Maximum number of errors to collect before stopping.
     * 0 = unlimited
     */
    private int maxErrors = 50;

    /**
     * List of supported HL7 v2 versions.
     */
    private java.util.List<String> supportedVersions = java.util.Arrays.asList("2.3", "2.3.1", "2.4", "2.5", "2.5.1",
            "2.6");

    /**
     * Whether to include the original HL7 segment text in error messages.
     */
    private boolean includeSegmentTextInErrors = false;

    public enum StrictnessLevel {
        STRICT,
        LENIENT,
        PERMISSIVE
    }

    public StrictnessLevel getStrictness() {
        return strictness;
    }

    public void setStrictness(StrictnessLevel strictness) {
        this.strictness = strictness;
    }

    public boolean isContinueOnError() {
        return continueOnError;
    }

    public void setContinueOnError(boolean continueOnError) {
        this.continueOnError = continueOnError;
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public void setValidationEnabled(boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    public boolean isFailOnValidationWarning() {
        return failOnValidationWarning;
    }

    public void setFailOnValidationWarning(boolean failOnValidationWarning) {
        this.failOnValidationWarning = failOnValidationWarning;
    }

    public boolean isIncludeOperationOutcome() {
        return includeOperationOutcome;
    }

    public void setIncludeOperationOutcome(boolean includeOperationOutcome) {
        this.includeOperationOutcome = includeOperationOutcome;
    }

    public int getMaxErrors() {
        return maxErrors;
    }

    public void setMaxErrors(int maxErrors) {
        this.maxErrors = maxErrors;
    }

    public java.util.List<String> getSupportedVersions() {
        return supportedVersions;
    }

    public void setSupportedVersions(java.util.List<String> supportedVersions) {
        this.supportedVersions = supportedVersions;
    }

    public boolean isIncludeSegmentTextInErrors() {
        return includeSegmentTextInErrors;
    }

    public void setIncludeSegmentTextInErrors(boolean includeSegmentTextInErrors) {
        this.includeSegmentTextInErrors = includeSegmentTextInErrors;
    }

    /**
     * Check if the current strictness allows continuing after an error
     */
    public boolean shouldContinueOnError() {
        return strictness != StrictnessLevel.STRICT && continueOnError;
    }

    /**
     * Check if we should treat a validation warning as an error
     */
    public boolean treatWarningAsError() {
        return strictness == StrictnessLevel.STRICT && failOnValidationWarning;
    }
}

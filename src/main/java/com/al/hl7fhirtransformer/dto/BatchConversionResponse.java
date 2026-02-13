package com.al.hl7fhirtransformer.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for batch conversion operations.
 * 
 * @author FHIR Transformer Team
 * @version 1.1.0
 * @since 1.1.0
 */
public class BatchConversionResponse {

    /**
     * Total number of messages in the batch.
     */
    private int totalMessages;

    /**
     * Number of successfully converted messages.
     */
    private int successCount;

    /**
     * Number of failed conversions.
     */
    private int failureCount;

    /**
     * Total processing time in milliseconds.
     */
    private long processingTimeMs;

    /**
     * List of successful conversion results.
     */
    private List<BatchItemResult> results = new ArrayList<>();

    /**
     * List of errors for failed conversions.
     */
    private List<BatchConversionErrorDetails> errors = new ArrayList<>();

    public BatchConversionResponse() {
    }

    public BatchConversionResponse(int totalMessages, int successCount, int failureCount, long processingTimeMs,
            List<BatchItemResult> results, List<BatchConversionErrorDetails> errors) {
        this.totalMessages = totalMessages;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.processingTimeMs = processingTimeMs;
        this.results = results;
        this.errors = errors;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(int totalMessages) {
        this.totalMessages = totalMessages;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public List<BatchItemResult> getResults() {
        return results;
    }

    public void setResults(List<BatchItemResult> results) {
        this.results = results;
    }

    public List<BatchConversionErrorDetails> getErrors() {
        return errors;
    }

    public void setErrors(List<BatchConversionErrorDetails> errors) {
        this.errors = errors;
    }

    /**
     * Individual conversion result.
     */
    public static class BatchItemResult {
        /**
         * Index of the message in the batch (0-based).
         */
        private int index;

        /**
         * Converted output (FHIR JSON or HL7 message).
         */
        private String output;

        /**
         * Processing time for this message in milliseconds.
         */
        private long processingTimeMs;

        /**
         * Message ID or transaction ID.
         */
        private String messageId;

        public BatchItemResult() {
        }

        public BatchItemResult(int index, String output, long processingTimeMs, String messageId) {
            this.index = index;
            this.output = output;
            this.processingTimeMs = processingTimeMs;
            this.messageId = messageId;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public long getProcessingTimeMs() {
            return processingTimeMs;
        }

        public void setProcessingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }
    }

    /**
     * Conversion error details.
     */
    public static class BatchConversionErrorDetails {
        /**
         * Index of the failed message in the batch (0-based).
         */
        private int index;

        /**
         * Error message.
         */
        private String error;

        /**
         * Original input that failed (truncated if too long).
         */
        private String input;

        public BatchConversionErrorDetails() {
        }

        public BatchConversionErrorDetails(int index, String error, String input) {
            this.index = index;
            this.error = error;
            this.input = input;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }
    }
}

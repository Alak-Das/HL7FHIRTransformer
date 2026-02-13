package com.al.hl7fhirtransformer.dto;

import java.time.LocalDateTime;

public class TransactionDTO {
    private String hl7fhirtransformerId; // Internal DB ID
    private String originalMessageId; // MSH-10 or Bundle.id
    private String messageType;
    private String status;
    private LocalDateTime timestamp;

    public TransactionDTO() {
    }

    public TransactionDTO(String hl7fhirtransformerId, String originalMessageId, String messageType, String status,
            LocalDateTime timestamp) {
        this.hl7fhirtransformerId = hl7fhirtransformerId;
        this.originalMessageId = originalMessageId;
        this.messageType = messageType;
        this.status = status;
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getHl7fhirtransformerId() {
        return hl7fhirtransformerId;
    }

    public void setHl7fhirtransformerId(String hl7fhirtransformerId) {
        this.hl7fhirtransformerId = hl7fhirtransformerId;
    }

    public String getOriginalMessageId() {
        return originalMessageId;
    }

    public void setOriginalMessageId(String originalMessageId) {
        this.originalMessageId = originalMessageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static class Builder {
        private String hl7fhirtransformerId;
        private String originalMessageId;
        private String messageType;
        private String status;
        private LocalDateTime timestamp;

        public Builder hl7fhirtransformerId(String hl7fhirtransformerId) {
            this.hl7fhirtransformerId = hl7fhirtransformerId;
            return this;
        }

        public Builder originalMessageId(String originalMessageId) {
            this.originalMessageId = originalMessageId;
            return this;
        }

        public Builder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public TransactionDTO build() {
            return new TransactionDTO(hl7fhirtransformerId, originalMessageId, messageType, status, timestamp);
        }
    }
}

package com.al.hl7fhirtransformer.dto;

public class EnrichedMessage {
    private String content; // The actual message payload (HL7 or FHIR JSON)
    private String transactionId;

    public EnrichedMessage() {
    }

    public EnrichedMessage(String content, String transactionId) {
        this.content = content;
        this.transactionId = transactionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}

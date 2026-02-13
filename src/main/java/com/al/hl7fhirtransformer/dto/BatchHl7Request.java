package com.al.hl7fhirtransformer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class BatchHl7Request {

    /**
     * List of HL7 v2.5 messages to convert.
     * Maximum 100 messages per batch to prevent memory issues.
     */
    @NotEmpty(message = "Messages list cannot be empty")
    @Size(min = 1, max = 100, message = "Batch size must be between 1 and 100 messages")
    private List<String> messages;

    /**
     * Optional tenant ID for multi-tenant scenarios.
     */
    private String tenantId;

    public BatchHl7Request() {
    }

    public BatchHl7Request(List<String> messages, String tenantId) {
        this.messages = messages;
        this.tenantId = tenantId;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}

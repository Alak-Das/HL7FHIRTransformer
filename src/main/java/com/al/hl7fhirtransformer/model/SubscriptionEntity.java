package com.al.hl7fhirtransformer.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

public class SubscriptionEntity {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String criteria; // e.g. "Patient?gender=male"
    private String status; // active, off, error
    private String channelType; // rest-hook, websocket
    private String endpoint; // Webhook URL
    private String payloadMimeType; // application/fhir+json

    private LocalDateTime createdDate;
    private LocalDateTime lastUpdatedDate;

    public SubscriptionEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCriteria() {
        return criteria;
    }

    public void setCriteria(String criteria) {
        this.criteria = criteria;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPayloadMimeType() {
        return payloadMimeType;
    }

    public void setPayloadMimeType(String payloadMimeType) {
        this.payloadMimeType = payloadMimeType;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(LocalDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
}

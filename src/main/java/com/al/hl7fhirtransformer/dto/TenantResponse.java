package com.al.hl7fhirtransformer.dto;

/**
 * Response DTO for Tenant API endpoints.
 * Excludes sensitive fields like password hash.
 */
public class TenantResponse {
    private String id;
    private String tenantId;
    private String name;
    private Integer requestLimitPerMinute;

    public TenantResponse() {
    }

    public TenantResponse(String id, String tenantId, String name, Integer requestLimitPerMinute) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.requestLimitPerMinute = requestLimitPerMinute;
    }

    /**
     * Create a TenantResponse from a Tenant entity.
     */
    public static TenantResponse from(com.al.hl7fhirtransformer.model.Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getTenantId(),
                tenant.getName(),
                tenant.getRequestLimitPerMinute());
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRequestLimitPerMinute() {
        return requestLimitPerMinute;
    }

    public void setRequestLimitPerMinute(Integer requestLimitPerMinute) {
        this.requestLimitPerMinute = requestLimitPerMinute;
    }
}

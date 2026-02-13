package com.al.hl7fhirtransformer.dto;

import jakarta.validation.constraints.NotBlank;

public class TenantOnboardRequest {
    @NotBlank(message = "Tenant ID cannot be blank")
    private String tenantId;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    private String name;

    // Optional: Rate limit in requests per minute (default: 60)
    private Integer requestLimitPerMinute = 60;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

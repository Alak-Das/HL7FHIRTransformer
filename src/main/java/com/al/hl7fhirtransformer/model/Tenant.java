package com.al.hl7fhirtransformer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "tenants")
public class Tenant {
    @Id
    private String id;
    private String tenantId;
    @JsonIgnore
    private String password;
    private String name;

    // Rate limiting: requests per minute (default: 60)
    private Integer requestLimitPerMinute = 60;

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

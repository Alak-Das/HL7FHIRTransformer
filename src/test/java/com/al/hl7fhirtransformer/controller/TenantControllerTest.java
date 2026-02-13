package com.al.hl7fhirtransformer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.al.hl7fhirtransformer.dto.TenantOnboardRequest;
import com.al.hl7fhirtransformer.model.Tenant;
import com.al.hl7fhirtransformer.service.TenantService;
import com.al.hl7fhirtransformer.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(TenantController.class)
// if
// needed,
// or
// rely
// on
// defaults

public class TenantControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private TenantService tenantService;

        @MockBean
        private TransactionService transactionService;

        @MockBean
        private com.al.hl7fhirtransformer.service.RateLimitService rateLimitService;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        public void testOnboardTenant_Success() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("tenantId", "tenant1");
                request.put("password", "password123");
                request.put("name", "Test Hospital");

                Tenant tenant = new Tenant();
                tenant.setTenantId("tenant1");
                tenant.setName("Test Hospital");

                when(tenantService.onboardTenant(any(TenantOnboardRequest.class))).thenReturn(tenant);

                mockMvc.perform(post("/api/tenants/onboard")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.tenantId").value("tenant1"))
                                .andExpect(jsonPath("$.name").value("Test Hospital"));
        }

        @Test
        public void testOnboardTenant_Unauthorized() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("tenantId", "tenant1");
                request.put("password", "password123");
                request.put("name", "Test Hospital");

                mockMvc.perform(post("/api/tenants/onboard")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        public void testGetTenantTransactions_HandlesNullStatus() throws Exception {
                String tenantId = "tenant1";
                LocalDateTime now = LocalDateTime.now();

                // Mock Page result
                org.springframework.data.domain.Page<com.al.hl7fhirtransformer.model.TransactionRecord> page = org.springframework.data.domain.Page
                                .empty();
                when(transactionService.findByTenantIdAndTimestampBetween(
                                (String) org.mockito.ArgumentMatchers.eq(tenantId),
                                org.mockito.ArgumentMatchers.any(),
                                org.mockito.ArgumentMatchers.any(),
                                org.mockito.ArgumentMatchers.any()))
                                .thenReturn(page);

                // Mock StatusCounts with a NULL ID
                java.util.List<com.al.hl7fhirtransformer.dto.StatusCount> statusCounts = new java.util.ArrayList<>();
                statusCounts.add(new com.al.hl7fhirtransformer.dto.StatusCount(null, 5)); // The problematic case
                statusCounts.add(new com.al.hl7fhirtransformer.dto.StatusCount("COMPLETED", 10));

                when(transactionService.countStatusByTenantIdAndTimestampBetween(
                                (String) org.mockito.ArgumentMatchers.eq(tenantId),
                                org.mockito.ArgumentMatchers.any(),
                                org.mockito.ArgumentMatchers.any()))
                                .thenReturn(statusCounts);

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/tenants/{tenantId}/transactions", tenantId)
                                .param("startDate", now.minusDays(1).toString())
                                .param("endDate", now.plusDays(1).toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.statusCounts.UNKNOWN").value(5)) // Verify mapping of null ->
                                                                                        // UNKNOWN
                                .andExpect(jsonPath("$.statusCounts.COMPLETED").value(10));
        }
}

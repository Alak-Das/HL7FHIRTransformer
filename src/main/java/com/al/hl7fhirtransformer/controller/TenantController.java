package com.al.hl7fhirtransformer.controller;

import com.al.hl7fhirtransformer.dto.TransactionSummaryResponse;
import com.al.hl7fhirtransformer.dto.TransactionDTO;
import com.al.hl7fhirtransformer.dto.TenantOnboardRequest;
import com.al.hl7fhirtransformer.dto.TenantResponse;
import com.al.hl7fhirtransformer.dto.TenantUpdateRequest;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.al.hl7fhirtransformer.dto.StatusCount;
import java.util.Map;
import com.al.hl7fhirtransformer.model.Tenant;
import com.al.hl7fhirtransformer.model.TransactionRecord;
import com.al.hl7fhirtransformer.service.TenantService;
import com.al.hl7fhirtransformer.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenants", description = "Tenant management and transaction history")
public class TenantController {
    private static final Logger log = LoggerFactory.getLogger(TenantController.class);

    private final TenantService tenantService;
    private final TransactionService transactionService;

    @Autowired
    public TenantController(TenantService tenantService, TransactionService transactionService) {
        this.tenantService = tenantService;
        this.transactionService = transactionService;
    }

    @Operation(summary = "List Tenants", description = "Get a list of all onboarded tenants.")
    @GetMapping
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        List<TenantResponse> responses = tenantService.getAllTenants().stream()
                .map(TenantResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get Tenant Transactions", description = "Retrieve a paginated list of transactions for a specific tenant within a date range.")
    @GetMapping("/{tenantId}/transactions")
    public ResponseEntity<TransactionSummaryResponse> getTenantTransactions(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "Start date (ISO 8601)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO 8601)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Page<TransactionRecord> pageRecords = transactionService.findByTenantIdAndTimestampBetween(
                tenantId, startDate, endDate, PageRequest.of(page, size, Sort.by("timestamp").descending()));

        List<StatusCount> statusStats = transactionService.countStatusByTenantIdAndTimestampBetween(
                tenantId, startDate, endDate);

        Map<String, Long> statusCounts = statusStats.stream()
                .collect(Collectors.toMap(
                        sc -> sc.get_id() != null ? sc.get_id() : "UNKNOWN",
                        StatusCount::getCount,
                        (c1, c2) -> c1 + c2));

        List<TransactionDTO> dtos = pageRecords.getContent().stream()
                .map(r -> TransactionDTO.builder()
                        .hl7fhirtransformerId(r.getId())
                        .originalMessageId(r.getTransactionId())
                        .messageType(r.getMessageType())
                        .status(r.getStatus())
                        .timestamp(r.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(TransactionSummaryResponse.builder()
                .totalCount(pageRecords.getTotalElements())
                .totalPages(pageRecords.getTotalPages())
                .currentPage(pageRecords.getNumber())
                .statusCounts(statusCounts)
                .transactions(dtos)
                .build());
    }

    /**
     * Retrieve a single transaction record for a tenant by its transactionId.
     * Useful for polling the status of async conversions using the transformerId
     * header value.
     * GET /api/tenants/{tenantId}/transactions/{transactionId}
     */
    @Operation(summary = "Get Transaction", description = "Retrieve a single transaction record for a tenant by its transactionId.")
    @GetMapping("/{tenantId}/transactions/{transactionId}")
    public ResponseEntity<TransactionDTO> getTransaction(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Parameter(description = "Transaction ID") @PathVariable String transactionId) {

        return transactionService.findByTenantIdAndTransactionId(tenantId, transactionId)
                .map(r -> TransactionDTO.builder()
                        .hl7fhirtransformerId(r.getId())
                        .originalMessageId(r.getTransactionId())
                        .messageType(r.getMessageType())
                        .status(r.getStatus())
                        .timestamp(r.getTimestamp())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Onboard a new tenant with optional rate limit configuration.
     */
    @Operation(summary = "Onboard Tenant", description = "Onboard a new tenant with optional rate limit configuration.")
    @PostMapping("/onboard")
    public ResponseEntity<TenantResponse> onboardTenant(@Valid @RequestBody TenantOnboardRequest request) {
        log.info("Received request to onboard tenant: {}", request.getTenantId());
        Tenant tenant = tenantService.onboardTenant(request);
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    @Operation(summary = "Update Tenant", description = "Update an existing tenant's details.")
    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> updateTenant(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId,
            @Valid @RequestBody TenantUpdateRequest request) {
        Tenant tenant = tenantService.updateTenant(tenantId, request.getPassword(), request.getName());
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    @Operation(summary = "Delete Tenant", description = "Remove a tenant from the system.")
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<String> deleteTenant(@Parameter(description = "Tenant ID") @PathVariable String tenantId) {
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.ok("Tenant deleted successfully");
    }
}

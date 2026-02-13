package com.al.hl7fhirtransformer.controller;

import com.al.hl7fhirtransformer.dto.TransactionSummaryResponse;
import com.al.hl7fhirtransformer.dto.TransactionDTO;
import com.al.hl7fhirtransformer.dto.TenantOnboardRequest;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private static final Logger log = LoggerFactory.getLogger(TenantController.class);

    private final TenantService tenantService;
    private final TransactionService transactionService;

    @Autowired
    public TenantController(TenantService tenantService, TransactionService transactionService) {
        this.tenantService = tenantService;
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    @GetMapping("/{tenantId}/transactions")
    public ResponseEntity<TransactionSummaryResponse> getTenantTransactions(
            @PathVariable String tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

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
     * Onboard a new tenant with optional rate limit configuration.
     */
    @PostMapping("/onboard")
    public ResponseEntity<Tenant> onboardTenant(@Valid @RequestBody TenantOnboardRequest request) {
        log.info("Received request to onboard tenant: {}", request.getTenantId());
        Tenant tenant = tenantService.onboardTenant(request);
        return ResponseEntity.ok(tenant);
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<Tenant> updateTenant(@PathVariable String tenantId,
            @Valid @RequestBody TenantUpdateRequest request) {
        Tenant tenant = tenantService.updateTenant(tenantId, request.getPassword(), request.getName());
        return ResponseEntity.ok(tenant);
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<String> deleteTenant(@PathVariable String tenantId) {
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.ok("Tenant deleted successfully");
    }
}

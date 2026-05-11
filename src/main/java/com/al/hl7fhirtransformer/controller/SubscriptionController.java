package com.al.hl7fhirtransformer.controller;

import com.al.hl7fhirtransformer.model.SubscriptionEntity;
import com.al.hl7fhirtransformer.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Webhook subscription management")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Create a new webhook subscription for a tenant.
     * POST /api/subscriptions?criteria=Patient&endpoint=http://host/hook
     */
    @Operation(summary = "Create Subscription", description = "Create a new webhook subscription for a tenant. Examples of criteria: 'Patient', 'Patient?gender=male'")
    @PostMapping
    public ResponseEntity<SubscriptionEntity> createSubscription(
            @Parameter(description = "FHIR Resource type or criteria") @RequestParam String criteria,
            @Parameter(description = "Webhook URL to send notifications to") @RequestParam String endpoint,
            @RequestHeader(value = "tenantId", required = false) String tenantId) {

        String activeTenantId = (tenantId != null) ? tenantId : "default";
        SubscriptionEntity subscription = subscriptionService.createSubscription(activeTenantId, criteria, endpoint);
        return ResponseEntity.ok(subscription);
    }

    /**
     * List all active subscriptions for a tenant.
     * GET /api/subscriptions
     */
    @Operation(summary = "List Subscriptions", description = "List all active webhook subscriptions for a tenant.")
    @GetMapping
    public ResponseEntity<List<SubscriptionEntity>> getSubscriptions(
            @RequestHeader(value = "tenantId", required = false) String tenantId) {

        String activeTenantId = (tenantId != null) ? tenantId : "default";
        List<SubscriptionEntity> subscriptions = subscriptionService.getActiveSubscriptions(activeTenantId);
        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Update an existing subscription's criteria and/or endpoint.
     * PUT
     * /api/subscriptions/{id}?criteria=Patient?gender=male&endpoint=http://new-host/hook
     */
    @Operation(summary = "Update Subscription", description = "Update an existing subscription's criteria and/or endpoint.")
    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionEntity> updateSubscription(
            @Parameter(description = "Subscription ID") @PathVariable String id,
            @Parameter(description = "New criteria (optional)") @RequestParam(required = false) String criteria,
            @Parameter(description = "New webhook URL (optional)") @RequestParam(required = false) String endpoint,
            @RequestHeader(value = "tenantId", required = false) String tenantId) {

        String activeTenantId = (tenantId != null) ? tenantId : "default";
        try {
            SubscriptionEntity updated = subscriptionService.updateSubscription(id, criteria, endpoint, activeTenantId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cancel (soft-delete) a subscription.
     * DELETE /api/subscriptions/{id}
     */
    @Operation(summary = "Cancel Subscription", description = "Cancel (soft-delete) a webhook subscription.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> cancelSubscription(
            @Parameter(description = "Subscription ID") @PathVariable String id,
            @RequestHeader(value = "tenantId", required = false) String tenantId) {

        String activeTenantId = (tenantId != null) ? tenantId : "default";
        try {
            subscriptionService.cancelSubscription(id, activeTenantId);
            return ResponseEntity.ok(Map.of("message", "Subscription cancelled successfully", "id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

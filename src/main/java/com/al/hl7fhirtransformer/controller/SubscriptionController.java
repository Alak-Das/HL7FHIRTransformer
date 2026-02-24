package com.al.hl7fhirtransformer.controller;

import com.al.hl7fhirtransformer.model.SubscriptionEntity;
import com.al.hl7fhirtransformer.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Create a new webhook subscription for a tenant.
     * POST /api/subscriptions?criteria=Patient&endpoint=http://host/hook
     */
    @PostMapping
    public ResponseEntity<SubscriptionEntity> createSubscription(
            @RequestParam String criteria,
            @RequestParam String endpoint,
            @RequestHeader(value = "tenantId", required = false) String tenantId) {

        String activeTenantId = (tenantId != null) ? tenantId : "default";
        SubscriptionEntity subscription = subscriptionService.createSubscription(activeTenantId, criteria, endpoint);
        return ResponseEntity.ok(subscription);
    }

    /**
     * List all active subscriptions for a tenant.
     * GET /api/subscriptions
     */
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
    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionEntity> updateSubscription(
            @PathVariable String id,
            @RequestParam(required = false) String criteria,
            @RequestParam(required = false) String endpoint,
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
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> cancelSubscription(
            @PathVariable String id,
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

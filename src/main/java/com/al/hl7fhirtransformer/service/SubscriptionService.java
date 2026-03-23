package com.al.hl7fhirtransformer.service;

import com.al.hl7fhirtransformer.model.SubscriptionEntity;
import com.al.hl7fhirtransformer.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final WebhookService webhookService;
    private final ca.uhn.fhir.context.FhirContext fhirContext;

    public SubscriptionEntity createSubscription(String tenantId, String criteria, String endpoint) {
        validateEndpointUrl(endpoint);
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setTenantId(tenantId);
        entity.setCriteria(criteria);
        entity.setEndpoint(endpoint);
        entity.setStatus("active");
        entity.setChannelType("rest-hook");
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastUpdatedDate(LocalDateTime.now());
        return subscriptionRepository.save(entity);
    }

    public List<SubscriptionEntity> getActiveSubscriptions(String tenantId) {
        return subscriptionRepository.findByTenantIdAndStatus(tenantId, "active");
    }

    /**
     * Update an existing subscription's criteria and/or endpoint.
     *
     * @param id       Subscription ID
     * @param criteria New criteria (e.g. "Patient?gender=male"), or null to keep
     *                 existing
     * @param endpoint New webhook endpoint URL, or null to keep existing
     * @param tenantId Tenant ID (for ownership validation)
     * @return Updated SubscriptionEntity
     * @throws IllegalArgumentException if not found or not owned by tenant
     */
    public SubscriptionEntity updateSubscription(String id, String criteria, String endpoint, String tenantId) {
        SubscriptionEntity entity = subscriptionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + id + " for tenant: " + tenantId));

        if (criteria != null && !criteria.isBlank()) {
            entity.setCriteria(criteria);
        }
        if (endpoint != null && !endpoint.isBlank()) {
            validateEndpointUrl(endpoint);
            entity.setEndpoint(endpoint);
        }
        entity.setLastUpdatedDate(LocalDateTime.now());
        log.info("Updated subscription {} for tenant {}", id, tenantId);
        return subscriptionRepository.save(entity);
    }

    /**
     * Cancel (soft-delete) a subscription by setting its status to "cancelled".
     *
     * @param id       Subscription ID
     * @param tenantId Tenant ID (for ownership validation)
     * @throws IllegalArgumentException if not found or not owned by tenant
     */
    public void cancelSubscription(String id, String tenantId) {
        SubscriptionEntity entity = subscriptionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + id + " for tenant: " + tenantId));

        entity.setStatus("cancelled");
        entity.setLastUpdatedDate(LocalDateTime.now());
        subscriptionRepository.save(entity);
        log.info("Cancelled subscription {} for tenant {}", id, tenantId);
    }

    /**
     * Check if any resources in the Bundle match active subscriptions and notify.
     * Uses parallel processing for improved performance with multiple
     * subscriptions.
     */
    @org.springframework.scheduling.annotation.Async
    public void checkAndNotify(Bundle bundle, String tenantId) {
        List<SubscriptionEntity> subscriptions = getActiveSubscriptions(tenantId);
        if (subscriptions.isEmpty())
            return;

        List<Resource> resources = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .collect(Collectors.toList());

        subscriptions.parallelStream().forEach(sub -> {
            try {
                if (matchesCriteria(sub.getCriteria(), resources)) {
                    log.info("Subscription {} matched for tenant {}. Sending notification to {}",
                            sub.getId(), tenantId, sub.getEndpoint());

                    Map<String, Object> details = Map.of(
                            "subscriptionId", sub.getId(),
                            "criteria", sub.getCriteria(),
                            "event", "resource_created");

                    webhookService.sendNotification(sub.getEndpoint(), "SUB-" + System.currentTimeMillis(),
                            "MATCHED", "FHIR-SUBSCRIPTION", details);
                }
            } catch (Exception e) {
                log.error("Error processing subscription {}: {}", sub.getId(), e.getMessage());
            }
        });
    }

    /**
     * Match a subscription criteria string against the list of resources in the
     * bundle.
     * Supports:
     * - ResourceType only, e.g. "Patient"
     * - ResourceType with simple key=value params, e.g. "Patient?gender=male"
     * - Multiple params joined by &, e.g.
     * "Observation?status=final&category=vital-signs"
     *
     * Parameter values are checked against the resource's JSON representation.
     */
    private boolean matchesCriteria(String criteria, List<Resource> resources) {
        if (criteria == null || criteria.isEmpty())
            return false;

        String[] parts = criteria.split("\\?", 2);
        String resourceType = parts[0].trim();

        List<Resource> matchingTypeResources = resources.stream()
                .filter(r -> r != null && r.getResourceType().name().equalsIgnoreCase(resourceType))
                .collect(Collectors.toList());

        if (matchingTypeResources.isEmpty()) {
            return false;
        }

        // No parameters — resource type match alone is enough
        if (parts.length == 1 || parts[1].isBlank()) {
            return true;
        }

        // Parse query parameters
        Map<String, String> params = parseQueryParams(parts[1]);
        if (params.isEmpty()) {
            return true;
        }

        // Check each resource's JSON for presence of all param key+value pairs
        for (Resource resource : matchingTypeResources) {
            try {
                String resourceJson = fhirContext.newJsonParser().encodeResourceToString(resource);
                boolean allParamsMatch = params.entrySet().stream()
                        .allMatch(e -> resourceJson.contains("\"" + e.getKey() + "\"")
                                && resourceJson.contains("\"" + e.getValue() + "\""));
                if (allParamsMatch) {
                    return true;
                }
            } catch (Exception e) {
                log.debug("Error checking resource parameters for subscription: {}", e.getMessage());
            }
        }

        return false;
    }

    /** Parse "key1=val1&key2=val2" into a map. */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new java.util.LinkedHashMap<>();
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && !kv[0].isBlank() && !kv[1].isBlank()) {
                params.put(kv[0].trim(), kv[1].trim());
            }
        }
        return params;
    }

    /**
     * Validate a webhook endpoint URL for format and SSRF protection.
     * Blocks localhost, loopback, link-local, and private IP ranges.
     *
     * @param endpoint The URL to validate
     * @throws IllegalArgumentException if the URL is invalid or points to an internal address
     */
    private void validateEndpointUrl(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("Subscription endpoint URL cannot be empty");
        }

        java.net.URI uri;
        try {
            uri = java.net.URI.create(endpoint);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid subscription endpoint URL: " + endpoint);
        }

        String protocol = uri.getScheme();
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            throw new IllegalArgumentException("Subscription endpoint must use HTTP or HTTPS protocol");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Subscription endpoint URL must have a valid host");
        }
        host = host.toLowerCase();

        // Block loopback and localhost
        if ("localhost".equals(host) || host.startsWith("127.") || "::1".equals(host)) {
            throw new IllegalArgumentException("Subscription endpoint cannot point to localhost");
        }

        // Block link-local addresses
        if (host.startsWith("169.254.")) {
            throw new IllegalArgumentException("Subscription endpoint cannot point to link-local address");
        }

        // Block private IP ranges
        if (host.startsWith("10.") || host.startsWith("192.168.")) {
            throw new IllegalArgumentException("Subscription endpoint cannot point to private IP address");
        }

        // Block 172.16.0.0 - 172.31.255.255
        if (host.startsWith("172.")) {
            try {
                String[] parts = host.split("\\.");
                if (parts.length >= 2) {
                    int secondOctet = Integer.parseInt(parts[1]);
                    if (secondOctet >= 16 && secondOctet <= 31) {
                        throw new IllegalArgumentException(
                                "Subscription endpoint cannot point to private IP address");
                    }
                }
            } catch (NumberFormatException ignored) {
                // Not a numeric IP, let it pass
            }
        }
    }
}

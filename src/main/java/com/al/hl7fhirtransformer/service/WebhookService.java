package com.al.hl7fhirtransformer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending webhook notifications on message processing events.
 * Supports configurable retry with exponential backoff on delivery failure.
 */
@Service
@RequiredArgsConstructor
public class WebhookService {
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.webhook.max-retries:3}")
    private int maxRetries;

    @Value("${app.webhook.retry-delay-ms:1000}")
    private long retryDelayMs;

    /**
     * Send a webhook notification asynchronously with retry logic.
     * 
     * @param webhookUrl    The URL to send the notification to
     * @param transactionId The transaction ID
     * @param status        The processing status (COMPLETED, FAILED, etc.)
     * @param messageType   The message type (ADT^A01, ORU^R01, etc.)
     * @param details       Additional details (error message, result summary, etc.)
     */
    @Async
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "webhook", fallbackMethod = "webhookFallback")
    @Retryable(
            retryFor = { Exception.class },
            maxAttemptsExpression = "${app.webhook.max-retries:3}",
            backoff = @Backoff(delayExpression = "${app.webhook.retry-delay-ms:1000}", multiplier = 2))
    public void sendNotification(String webhookUrl, String transactionId, String status,
            String messageType, Map<String, Object> details) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.debug("No webhook URL configured, skipping notification for transaction {}", transactionId);
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("transactionId", transactionId);
            payload.put("status", status);
            payload.put("messageType", messageType);
            payload.put("timestamp", LocalDateTime.now().toString());

            if (details != null) {
                payload.put("details", details);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Transaction-Id", transactionId);
            headers.set("X-Event-Type", "message.processed");

            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            log.info("Sending webhook notification to {} for transaction {}", webhookUrl, transactionId);

            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Webhook notification sent successfully for transaction {}, status: {}",
                        transactionId, response.getStatusCode());
            } else {
                log.warn("Webhook notification returned non-success status {} for transaction {}",
                        response.getStatusCode(), transactionId);
                throw new RuntimeException("Non-success status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to send webhook notification for transaction {}: {}",
                    transactionId, e.getMessage());
            throw new RuntimeException("Webhook delivery failed", e); // Let Spring Retry handle it
        }
    }

    /**
     * Send a completion notification
     */
    public void notifyCompletion(String webhookUrl, String transactionId, String messageType,
            int resourceCount) {
        Map<String, Object> details = new HashMap<>();
        details.put("resourceCount", resourceCount);
        details.put("result", "success");

        sendNotification(webhookUrl, transactionId, "COMPLETED", messageType, details);
    }

    /**
     * Send a failure notification
     */
    public void notifyFailure(String webhookUrl, String transactionId, String messageType,
            String errorMessage, int retryCount) {
        Map<String, Object> details = new HashMap<>();
        details.put("error", errorMessage);
        details.put("retryCount", retryCount);
        details.put("result", "failure");

        sendNotification(webhookUrl, transactionId, "FAILED", messageType, details);
    }

    /**
     * Send a partial success notification
     */
    public void notifyPartialSuccess(String webhookUrl, String transactionId, String messageType,
            int successCount, int errorCount) {
        Map<String, Object> details = new HashMap<>();
        details.put("successCount", successCount);
        details.put("errorCount", errorCount);
        details.put("result", "partial");

        sendNotification(webhookUrl, transactionId, "PARTIAL", messageType, details);
    }

    /**
     * Fallback method called when the circuit breaker is open.
     */
    public void webhookFallback(String webhookUrl, String transactionId, String status,
            String messageType, Map<String, Object> details, Throwable t) {
        log.error("Circuit breaker is OPEN. Dropping webhook notification to {} for transaction {}. Reason: {}", 
                webhookUrl, transactionId, t.getMessage());
        // In a real production system, we might route this directly to a Dead Letter Queue 
        // to save memory, rather than trying to process it.
    }
}

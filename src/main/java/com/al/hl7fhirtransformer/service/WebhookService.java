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
    public void sendNotification(String webhookUrl, String transactionId, String status,
            String messageType, Map<String, Object> details) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.debug("No webhook URL configured, skipping notification for transaction {}", transactionId);
            return;
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
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

                log.info("Sending webhook notification to {} for transaction {} (attempt {}/{})",
                        webhookUrl, transactionId, attempt, maxRetries);

                ResponseEntity<String> response = restTemplate.exchange(
                        webhookUrl,
                        HttpMethod.POST,
                        request,
                        String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Webhook notification sent successfully for transaction {}, status: {}",
                            transactionId, response.getStatusCode());
                    return; // Success — exit retry loop
                } else {
                    log.warn("Webhook notification returned non-success status {} for transaction {} (attempt {}/{})",
                            response.getStatusCode(), transactionId, attempt, maxRetries);
                }

            } catch (Exception e) {
                log.error("Failed to send webhook notification for transaction {} (attempt {}/{}): {}",
                        transactionId, attempt, maxRetries, e.getMessage());

                if (attempt == maxRetries) {
                    log.error("All {} webhook delivery attempts exhausted for transaction {}. Notification dropped.",
                            maxRetries, transactionId);
                    return; // Don't rethrow — webhook failures should not affect main processing
                }
            }

            // Exponential backoff: delay * 2^(attempt-1)
            try {
                long delay = retryDelayMs * (1L << (attempt - 1));
                log.debug("Waiting {}ms before webhook retry attempt {}", delay, attempt + 1);
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Webhook retry interrupted for transaction {}", transactionId);
                return;
            }
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
}

package com.al.hl7fhirtransformer.listener;

import com.al.hl7fhirtransformer.service.FhirToHl7Service;
import com.al.hl7fhirtransformer.service.WebhookService;
import com.al.hl7fhirtransformer.service.AuditService;
import com.al.hl7fhirtransformer.config.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FhirMessageListener {
    private static final Logger log = LoggerFactory.getLogger(FhirMessageListener.class);

    private final FhirToHl7Service fhirToHl7Service;
    private final RabbitTemplate rabbitTemplate;
    private final AuditService auditService;
    private final WebhookService webhookService;

    @Value("${app.rabbitmq.v2.output-queue}")
    private String v2OutputQueue;

    @Value("${app.webhook.url:#{null}}")
    private String defaultWebhookUrl;

    @Autowired
    public FhirMessageListener(FhirToHl7Service fhirToHl7Service, RabbitTemplate rabbitTemplate,
            AuditService auditService, WebhookService webhookService) {
        this.fhirToHl7Service = fhirToHl7Service;
        this.rabbitTemplate = rabbitTemplate;
        this.auditService = auditService;
        this.webhookService = webhookService;
    }

    @RabbitListener(queues = "${app.rabbitmq.fhir.queue}")
    public void receiveMessage(
            String fhirJson,
            @org.springframework.messaging.handler.annotation.Header(value = "tenantId", required = false) String tenantId,
            @org.springframework.messaging.handler.annotation.Header(value = "x-retry-count", required = false, defaultValue = "0") Integer retryCount) {
        try {
            // Propagate tenant context for multi-tenant isolation
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            log.info("Processing FHIR message with TenantID: {} (retry attempt: {})", tenantId, retryCount);

            String hl7Message = fhirToHl7Service.convertFhirToHl7(fhirJson);

            // Publish to Output Queue
            rabbitTemplate.convertAndSend(v2OutputQueue, hl7Message);
            log.info("Successfully converted and published to {}", v2OutputQueue);

            // Update Transaction Status and send webhook notification
            String transactionId = extractTransactionId(hl7Message);
            if (transactionId != null) {
                auditService.updateTransactionSuccess(transactionId, "COMPLETED");
                if (defaultWebhookUrl != null) {
                    webhookService.notifyCompletion(defaultWebhookUrl, transactionId, "FHIR_TO_V2", 0);
                }
            }

        } catch (Exception e) {
            log.error("Error processing FHIR Message (attempt {}): {}", retryCount, e.getMessage(), e);

            String transactionId = extractTransactionIdFromFhir(fhirJson);

            if (retryCount < 3) {
                // Route to appropriate retry queue
                int nextRetry = retryCount + 1;
                String retryRoutingKey = "fhir.retry." + nextRetry;

                // Track retry in DB
                if (transactionId != null) {
                    auditService.updateTransactionFailure(transactionId, "RETRYING", e.getMessage(), nextRetry);
                }

                rabbitTemplate.convertAndSend(
                        "fhir-messages-exchange",
                        retryRoutingKey,
                        fhirJson,
                        message -> {
                            message.getMessageProperties().setHeader("x-retry-count", nextRetry);
                            message.getMessageProperties().setHeader("tenantId", tenantId);
                            message.getMessageProperties().setHeader("x-first-failure-reason",
                                    e.getClass().getSimpleName());
                            return message;
                        });

                log.info("Message routed to retry queue '{}' (attempt {} of 3)", retryRoutingKey, nextRetry);
            } else {
                // Max retries exhausted — route to DLQ explicitly
                if (transactionId != null) {
                    auditService.updateTransactionFailure(transactionId, "FAILED", e.getMessage(), retryCount);
                    if (defaultWebhookUrl != null) {
                        webhookService.notifyFailure(defaultWebhookUrl, transactionId, "FHIR_TO_V2", e.getMessage(),
                                retryCount);
                    }
                }
                log.error("Max retries exhausted for FHIR message, transaction: {}. Routing to DLQ.", transactionId);
                rabbitTemplate.convertAndSend("fhir-to-v2-dlx", "fhir.message.dl", fhirJson, message -> {
                    message.getMessageProperties().setHeader("x-retry-count", retryCount);
                    message.getMessageProperties().setHeader("tenantId", tenantId);
                    message.getMessageProperties().setHeader("x-failure-reason", e.getMessage());
                    return message;
                });
            }
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Extract transaction ID from HL7 message (MSH-10 field)
     */
    private String extractTransactionId(String hl7Message) {
        try {
            String[] segments = hl7Message.split("\r");
            if (segments.length > 0) {
                String[] mshFields = segments[0].split("\\|", -1);
                if (mshFields.length > 9) {
                    return mshFields[9];
                }
            }
        } catch (Exception ex) {
            log.warn("Could not extract transaction ID from HL7: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Extract Bundle ID from FHIR JSON for audit tracking before conversion.
     */
    private String extractTransactionIdFromFhir(String fhirJson) {
        try {
            int idIndex = fhirJson.indexOf("\"id\"");
            if (idIndex > 0) {
                int valueStart = fhirJson.indexOf("\"", idIndex + 5) + 1;
                int valueEnd = fhirJson.indexOf("\"", valueStart);
                return fhirJson.substring(valueStart, valueEnd);
            }
        } catch (Exception ex) {
            log.debug("Could not extract transaction ID from FHIR JSON: {}", ex.getMessage());
        }
        return null;
    }
}

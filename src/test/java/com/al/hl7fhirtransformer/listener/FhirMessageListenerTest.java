package com.al.hl7fhirtransformer.listener;

import com.al.hl7fhirtransformer.model.TransactionRecord;
import com.al.hl7fhirtransformer.service.TransactionService;
import com.al.hl7fhirtransformer.service.AuditService;
import com.al.hl7fhirtransformer.model.enums.MessageType;
import com.al.hl7fhirtransformer.model.enums.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FhirMessageListenerTest extends BaseIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AuditService auditService;

    @Value("${app.rabbitmq.fhir.queue}")
    private String fhirQueue;

    @Test
    public void testSuccessfulFhirConversion() {
        String transactionId = "TEST-FHIR-ID-" + UUID.randomUUID().toString();
        String fhirMessage = "{\n" +
                "  \"resourceType\": \"Bundle\",\n" +
                "  \"id\": \"" + transactionId + "\",\n" +
                "  \"type\": \"message\",\n" +
                "  \"entry\": [\n" +
                "    {\n" +
                "      \"resource\": {\n" +
                "        \"resourceType\": \"MessageHeader\",\n" +
                "        \"eventCoding\": {\n" +
                "          \"system\": \"http://example.org/fhir/message-events\",\n" +
                "          \"code\": \"ADT^A01\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"resource\": {\n" +
                "        \"resourceType\": \"Patient\",\n" +
                "        \"name\": [{ \"family\": \"SMITH\", \"given\": [\"JANE\"] }]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Seed the transaction record
        auditService.logTransaction("tenant2", transactionId, MessageType.FHIR_TO_V2_ASYNC, TransactionStatus.QUEUED);

        // Send to queue
        rabbitTemplate.convertAndSend(fhirQueue, fhirMessage, message -> {
            message.getMessageProperties().setHeader("tenantId", "tenant2");
            return message;
        });

        // Wait for asynchronous processing
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<TransactionRecord> recordOpt = transactionService.findByTenantIdAndTransactionId("tenant2", transactionId);
            assertTrue(recordOpt.isPresent());
            assertEquals("COMPLETED", recordOpt.get().getStatus());
        });
    }
}

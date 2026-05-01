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

public class Hl7MessageListenerTest extends BaseIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AuditService auditService;

    @Value("${app.rabbitmq.queue}")
    private String hl7Queue;

    @Test
    public void testSuccessfulHl7Conversion() {
        String transactionId = "TEST-HL7-ID-" + UUID.randomUUID().toString();
        String hl7Message = "MSH|^~\\&|HIS|RIH|EKG|EkG|199904140038||ADT^A01|" + transactionId + "|P|2.5\r" +
                "PID|1||100||DOE^JOHN||19700101|M\r" +
                "PV1|1|I|2000^2012^01||||002970^FUSILIER^KAMERA^^^MD^Dr";

        // Seed the transaction record to simulate Controller creating it
        auditService.logTransaction("tenant1", transactionId, MessageType.V2_TO_FHIR_ASYNC, TransactionStatus.QUEUED);

        // Send to queue
        rabbitTemplate.convertAndSend(hl7Queue, hl7Message, message -> {
            message.getMessageProperties().setHeader("tenantId", "tenant1");
            return message;
        });

        // Wait for asynchronous processing
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<TransactionRecord> recordOpt = transactionService.findByTenantIdAndTransactionId("tenant1", transactionId);
            assertTrue(recordOpt.isPresent());
            assertEquals("COMPLETED", recordOpt.get().getStatus());
        });
    }
}

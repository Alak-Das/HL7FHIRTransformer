package com.al.hl7fhirtransformer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.al.hl7fhirtransformer.dto.EnrichedMessage;
import com.al.hl7fhirtransformer.model.enums.MessageType;
import com.al.hl7fhirtransformer.model.enums.TransactionStatus;
import com.al.hl7fhirtransformer.service.AuditService;
import com.al.hl7fhirtransformer.service.IdempotencyService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestHeader;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.al.hl7fhirtransformer.service.FhirToHl7Service;
import com.al.hl7fhirtransformer.service.Hl7ToFhirService;
import com.al.hl7fhirtransformer.service.MessageEnrichmentService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.al.hl7fhirtransformer.dto.BatchConversionResponse;
import com.al.hl7fhirtransformer.dto.BatchHl7Request;
import com.al.hl7fhirtransformer.service.BatchConversionService;
import com.al.hl7fhirtransformer.service.AckMessageService;

// Swagger imports removed due to compatibility issues

@RestController
@RequestMapping("/api/convert")
public class ConverterController {

    private static final Logger log = LoggerFactory.getLogger(ConverterController.class);

    private final Hl7ToFhirService hl7ToFhirService;
    private final FhirToHl7Service fhirToHl7Service;
    private final BatchConversionService batchConversionService;
    private final RabbitTemplate rabbitTemplate;
    private final MessageEnrichmentService messageEnrichmentService;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final AckMessageService ackMessageService;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routingkey}")
    private String routingKey;

    @Value("${app.rabbitmq.fhir.exchange}")
    private String fhirExchange;

    @Value("${app.rabbitmq.fhir.routingkey}")
    private String fhirRoutingKey;

    @Autowired
    public ConverterController(Hl7ToFhirService hl7ToFhirService, FhirToHl7Service fhirToHl7Service,
            BatchConversionService batchConversionService, RabbitTemplate rabbitTemplate,
            MessageEnrichmentService messageEnrichmentService, AuditService auditService,
            IdempotencyService idempotencyService, ObjectMapper objectMapper,
            AckMessageService ackMessageService) {
        this.hl7ToFhirService = hl7ToFhirService;
        this.fhirToHl7Service = fhirToHl7Service;
        this.batchConversionService = batchConversionService;
        this.rabbitTemplate = rabbitTemplate;
        this.messageEnrichmentService = messageEnrichmentService;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
        this.ackMessageService = ackMessageService;
    }

    @PostMapping(value = "/v2-to-fhir", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Map<String, String>> convertToFhir(@RequestBody String hl7Message, Principal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // Idempotency check — return existing transaction info on duplicate
        if (idempotencyKey != null && idempotencyService.isDuplicate(idempotencyKey)) {
            Map<String, String> cached = new HashMap<>();
            cached.put("status", "Already Processed");
            cached.put("message", "Request with this Idempotency-Key has already been processed");
            cached.put("idempotencyKey", idempotencyKey);
            return ResponseEntity.ok(cached);
        }

        String tenantId = principal != null ? principal.getName() : "default";
        EnrichedMessage enriched = messageEnrichmentService.ensureHl7TransactionId(hl7Message);
        String transactionId = enriched.getTransactionId();
        String messageWithId = enriched.getContent();

        auditService.logTransaction(tenantId, transactionId, MessageType.V2_TO_FHIR_ASYNC, TransactionStatus.ACCEPTED,
                idempotencyKey);

        // Send to RabbitMQ
        rabbitTemplate.convertAndSend(exchange, routingKey, messageWithId, message -> {
            message.getMessageProperties().setHeader("transformerId", transactionId);
            message.getMessageProperties().setHeader("tenantId", tenantId);
            return message;
        });

        Map<String, String> response = new HashMap<>();
        response.put("status", "Accepted");
        response.put("transactionId", transactionId);
        response.put("message", "Processing asynchronously");

        return ResponseEntity.accepted()
                .header("transformerId", transactionId)
                .body(response);
    }

    @PostMapping(value = "/v2-to-fhir-sync", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> convertToFhirSync(@RequestBody String hl7Message, Principal principal) {
        String tenantId = principal != null ? principal.getName() : "default";
        EnrichedMessage enriched = messageEnrichmentService.ensureHl7TransactionId(hl7Message);
        String transactionId = enriched.getTransactionId();
        String messageWithId = enriched.getContent();

        try {
            String fhirJson = hl7ToFhirService.convertHl7ToFhir(messageWithId);
            auditService.logTransaction(tenantId, transactionId, MessageType.V2_TO_FHIR_SYNC,
                    TransactionStatus.COMPLETED);
            return ResponseEntity.ok()
                    .header("transformerId", transactionId)
                    .body(fhirJson);
        } catch (IllegalArgumentException | ca.uhn.hl7v2.HL7Exception e) {
            auditService.logTransaction(tenantId, transactionId, MessageType.V2_TO_FHIR_SYNC, TransactionStatus.FAILED,
                    e.getMessage());
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (Exception e) {
            auditService.logTransaction(tenantId, transactionId, MessageType.V2_TO_FHIR_SYNC, TransactionStatus.FAILED,
                    e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/fhir-to-v2", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> convertToHl7(@RequestBody String fhirJson, Principal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // Idempotency check — return existing transaction info on duplicate
        if (idempotencyKey != null && idempotencyService.isDuplicate(idempotencyKey)) {
            Map<String, String> cached = new HashMap<>();
            cached.put("status", "Already Processed");
            cached.put("message", "Request with this Idempotency-Key has already been processed");
            cached.put("idempotencyKey", idempotencyKey);
            return ResponseEntity.ok(cached);
        }

        String tenantId = principal != null ? principal.getName() : "default";
        EnrichedMessage enriched;
        try {
            enriched = messageEnrichmentService.ensureFhirTransactionId(fhirJson);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid FHIR JSON"));
        }
        String transactionId = enriched.getTransactionId();
        String contentWithId = enriched.getContent();

        auditService.logTransaction(tenantId, transactionId, MessageType.FHIR_TO_V2_ASYNC, TransactionStatus.QUEUED,
                idempotencyKey);

        rabbitTemplate.convertAndSend(fhirExchange, fhirRoutingKey, contentWithId, message -> {
            message.getMessageProperties().setHeader("transformerId", transactionId);
            message.getMessageProperties().setHeader("tenantId", tenantId);
            return message;
        });

        Map<String, String> response = new HashMap<>();
        response.put("status", "Accepted");
        response.put("transactionId", transactionId);
        response.put("message", "Processing asynchronously");

        return ResponseEntity.accepted()
                .header("transformerId", transactionId)
                .body(response);
    }

    @PostMapping(value = "/fhir-to-v2-sync", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> convertToHl7Sync(@RequestBody String fhirJson, Principal principal) {
        String tenantId = principal != null ? principal.getName() : "default";
        EnrichedMessage enriched;
        try {
            enriched = messageEnrichmentService.ensureFhirTransactionId(fhirJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid FHIR JSON", e);
        }
        String transactionId = enriched.getTransactionId();
        String contentWithId = enriched.getContent();

        try {
            String hl7Message = fhirToHl7Service.convertFhirToHl7(contentWithId);
            auditService.logTransaction(tenantId, transactionId, MessageType.FHIR_TO_V2_SYNC,
                    TransactionStatus.COMPLETED);
            return ResponseEntity.ok()
                    .header("transformerId", transactionId)
                    .body(hl7Message);
        } catch (IllegalArgumentException e) {
            auditService.logTransaction(tenantId, transactionId, MessageType.FHIR_TO_V2_SYNC, TransactionStatus.FAILED,
                    e.getMessage());
            throw e;
        } catch (Exception e) {
            auditService.logTransaction(tenantId, transactionId, MessageType.FHIR_TO_V2_SYNC, TransactionStatus.FAILED,
                    e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/v2-to-fhir-batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchConversionResponse> processHl7Batch(@RequestBody BatchHl7Request request,
            Principal principal) {
        String tenantId = principal != null ? principal.getName() : "default";
        log.info("Batch HL7→FHIR request from tenant {}: {} messages", tenantId, request.getMessages().size());
        auditService.logTransaction(tenantId, "BATCH-" + System.currentTimeMillis(),
                MessageType.V2_TO_FHIR_SYNC, TransactionStatus.ACCEPTED);
        BatchConversionResponse response = batchConversionService.convertHl7ToFhirBatch(request.getMessages());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/fhir-to-v2-batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchConversionResponse> processFhirBatch(@RequestBody List<String> request,
            Principal principal) {
        String tenantId = principal != null ? principal.getName() : "default";
        log.info("Batch FHIR→HL7 request from tenant {}: {} bundles", tenantId, request.size());
        auditService.logTransaction(tenantId, "BATCH-" + System.currentTimeMillis(),
                MessageType.FHIR_TO_V2_SYNC, TransactionStatus.ACCEPTED);
        BatchConversionResponse response = batchConversionService.convertFhirToHl7Batch(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchConversionResponse> processBatch(@RequestBody BatchHl7Request request,
            Principal principal) {
        String tenantId = principal != null ? principal.getName() : "default";
        log.info("Batch request from tenant {}: {} messages", tenantId, request.getMessages().size());
        auditService.logTransaction(tenantId, "BATCH-" + System.currentTimeMillis(),
                MessageType.V2_TO_FHIR_SYNC, TransactionStatus.ACCEPTED);
        BatchConversionResponse response = batchConversionService.processBatch(request, tenantId);
        return ResponseEntity.ok(response);
    }
}

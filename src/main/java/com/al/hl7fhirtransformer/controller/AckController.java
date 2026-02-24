package com.al.hl7fhirtransformer.controller;

import com.al.hl7fhirtransformer.model.TransactionRecord;
import com.al.hl7fhirtransformer.service.AckMessageService;
import com.al.hl7fhirtransformer.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

/**
 * REST controller for retrieving HL7 v2 ACK messages for processed
 * transactions.
 *
 * <p>
 * After submitting a message via the async endpoints (POST
 * /api/convert/v2-to-fhir
 * or POST /api/convert/fhir-to-v2), callers receive a {@code transformerId}
 * header.
 * They can poll this endpoint to retrieve the corresponding HL7 ACK once
 * processing
 * is complete.
 *
 * <ul>
 * <li>COMPLETED → AA (Application Accept)</li>
 * <li>FAILED → AE (Application Error)</li>
 * <li>Everything else (ACCEPTED, QUEUED, PROCESSING) → AR (Application Reject /
 * not ready)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ack")
public class AckController {

    private static final Logger log = LoggerFactory.getLogger(AckController.class);

    private final TransactionService transactionService;
    private final AckMessageService ackMessageService;

    @Autowired
    public AckController(TransactionService transactionService, AckMessageService ackMessageService) {
        this.transactionService = transactionService;
        this.ackMessageService = ackMessageService;
    }

    /**
     * Retrieve an HL7 v2 ACK for a previously submitted async conversion.
     *
     * @param transactionId The {@code transformerId} returned as a response header
     *                      from POST /api/convert/v2-to-fhir or /fhir-to-v2
     * @param principal     Authenticated user (tenantId derived from principal
     *                      name)
     * @return HL7 pipe-delimited ACK string with Content-Type: text/plain, or 404
     *         if not found
     */
    @GetMapping(value = "/{transactionId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getAck(
            @PathVariable String transactionId,
            Principal principal) {

        String tenantId = principal != null ? principal.getName() : "default";
        log.info("ACK requested for transactionId={} by tenant={}", transactionId, tenantId);

        // Look up by tenantId + transactionId for security isolation
        Optional<TransactionRecord> recordOpt = transactionService.findByTenantIdAndTransactionId(tenantId,
                transactionId);

        // Fall back to global lookup (for backwards compat / admin use)
        if (recordOpt.isEmpty()) {
            recordOpt = transactionService.findByTransactionId(transactionId);
        }

        if (recordOpt.isEmpty()) {
            log.warn("No transaction found for id={}", transactionId);
            return ResponseEntity.notFound().build();
        }

        TransactionRecord record = recordOpt.get();
        try {
            String ack = ackMessageService.generateAckFromRecord(record);
            return ResponseEntity.ok()
                    .header("transformerId", transactionId)
                    .header("X-Ack-Code", deriveAckCode(record.getStatus()))
                    .body(ack);
        } catch (Exception e) {
            log.error("Failed to generate ACK for transactionId={}: {}", transactionId, e.getMessage());
            return ResponseEntity.internalServerError().body("ACK generation failed: " + e.getMessage());
        }
    }

    private String deriveAckCode(String status) {
        if ("COMPLETED".equalsIgnoreCase(status))
            return "AA";
        if ("FAILED".equalsIgnoreCase(status))
            return "AE";
        return "AR";
    }
}

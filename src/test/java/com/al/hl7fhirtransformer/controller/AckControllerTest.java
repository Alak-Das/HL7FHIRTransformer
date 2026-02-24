package com.al.hl7fhirtransformer.controller;

import com.al.hl7fhirtransformer.model.TransactionRecord;
import com.al.hl7fhirtransformer.service.AckMessageService;
import com.al.hl7fhirtransformer.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AckControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private AckMessageService ackMessageService;

    @InjectMocks
    private AckController controller;

    private TransactionRecord completedRecord;
    private TransactionRecord failedRecord;
    private TransactionRecord pendingRecord;
    private Principal principal;

    @BeforeEach
    void setUp() {
        principal = () -> "admin";

        completedRecord = new TransactionRecord();
        completedRecord.setId("mongo-id-1");
        completedRecord.setTransactionId("TX-COMPLETED-001");
        completedRecord.setTenantId("admin");
        completedRecord.setStatus("COMPLETED");
        completedRecord.setTimestamp(LocalDateTime.now());

        failedRecord = new TransactionRecord();
        failedRecord.setId("mongo-id-2");
        failedRecord.setTransactionId("TX-FAILED-001");
        failedRecord.setTenantId("admin");
        failedRecord.setStatus("FAILED");
        failedRecord.setLastErrorMessage("Parsing error in MSH segment");
        failedRecord.setTimestamp(LocalDateTime.now());

        pendingRecord = new TransactionRecord();
        pendingRecord.setId("mongo-id-3");
        pendingRecord.setTransactionId("TX-PENDING-001");
        pendingRecord.setTenantId("admin");
        pendingRecord.setStatus("ACCEPTED");
        pendingRecord.setTimestamp(LocalDateTime.now());
    }

    @Test
    void getAck_whenCompleted_shouldReturnAaAck() throws Exception {
        String aaAck = "MSH|^~\\&|FHIR-TRANSFORMER|TRANSFORM-FACILITY|||" +
                "20260224120000||ACK|ACK-12345678|P|2.5\r" +
                "MSA|AA|TX-COMPLETED-001\r";

        when(transactionService.findByTenantIdAndTransactionId("admin", "TX-COMPLETED-001"))
                .thenReturn(Optional.of(completedRecord));
        when(ackMessageService.generateAckFromRecord(completedRecord)).thenReturn(aaAck);

        ResponseEntity<String> response = controller.getAck("TX-COMPLETED-001", principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("MSA|AA|TX-COMPLETED-001"));
        assertEquals("AA", response.getHeaders().getFirst("X-Ack-Code"));
    }

    @Test
    void getAck_whenFailed_shouldReturnAeAck() throws Exception {
        String aeAck = "MSH|^~\\&|FHIR-TRANSFORMER|TRANSFORM-FACILITY|||" +
                "20260224120000||ACK|ACK-12345678|P|2.5\r" +
                "MSA|AE|TX-FAILED-001|Parsing error in MSH segment\r";

        when(transactionService.findByTenantIdAndTransactionId("admin", "TX-FAILED-001"))
                .thenReturn(Optional.of(failedRecord));
        when(ackMessageService.generateAckFromRecord(failedRecord)).thenReturn(aeAck);

        ResponseEntity<String> response = controller.getAck("TX-FAILED-001", principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("MSA|AE|TX-FAILED-001"));
        assertEquals("AE", response.getHeaders().getFirst("X-Ack-Code"));
    }

    @Test
    void getAck_whenPending_shouldReturnArAck() throws Exception {
        String arAck = "MSH|^~\\&|FHIR-TRANSFORMER|TRANSFORM-FACILITY|||" +
                "20260224120000||ACK|ACK-12345678|P|2.5\r" +
                "MSA|AR|TX-PENDING-001|Processing not yet complete. Current status: ACCEPTED\r";

        when(transactionService.findByTenantIdAndTransactionId("admin", "TX-PENDING-001"))
                .thenReturn(Optional.of(pendingRecord));
        when(ackMessageService.generateAckFromRecord(pendingRecord)).thenReturn(arAck);

        ResponseEntity<String> response = controller.getAck("TX-PENDING-001", principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("MSA|AR|TX-PENDING-001"));
        assertEquals("AR", response.getHeaders().getFirst("X-Ack-Code"));
    }

    @Test
    void getAck_whenNotFound_shouldReturn404() {
        when(transactionService.findByTenantIdAndTransactionId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(transactionService.findByTransactionId(anyString()))
                .thenReturn(Optional.empty());

        ResponseEntity<String> response = controller.getAck("NONEXISTENT-TX", principal);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getAck_withNullPrincipal_shouldUseDefaultTenant() throws Exception {
        String aaAck = "MSH|^~\\&|||\rMSA|AA|TX-001\r";

        when(transactionService.findByTenantIdAndTransactionId("default", "TX-001"))
                .thenReturn(Optional.of(completedRecord));
        when(ackMessageService.generateAckFromRecord(any())).thenReturn(aaAck);

        ResponseEntity<String> response = controller.getAck("TX-001", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAck_whenAckGenerationFails_shouldReturn500() throws Exception {
        when(transactionService.findByTenantIdAndTransactionId("admin", "TX-COMPLETED-001"))
                .thenReturn(Optional.of(completedRecord));
        when(ackMessageService.generateAckFromRecord(any()))
                .thenThrow(new RuntimeException("HAPI context error"));

        ResponseEntity<String> response = controller.getAck("TX-COMPLETED-001", principal);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("ACK generation failed"));
    }
}

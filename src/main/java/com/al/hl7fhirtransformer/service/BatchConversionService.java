package com.al.hl7fhirtransformer.service;

import com.al.hl7fhirtransformer.dto.BatchConversionResponse;
import com.al.hl7fhirtransformer.dto.BatchConversionResponse.BatchItemResult;
import com.al.hl7fhirtransformer.dto.BatchHl7Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for batch conversion operations with parallel processing.
 * 
 * <p>
 * Processes multiple messages in parallel using a thread pool for optimal
 * performance.
 * Handles both HL7 to FHIR and FHIR to HL7 batch conversions.
 * 
 * @author FHIR Transformer Team
 * @version 1.2.0
 * @since 1.1.0
 */
@Service
public class BatchConversionService {
    private static final Logger log = LoggerFactory.getLogger(BatchConversionService.class);

    private final Hl7ToFhirService hl7ToFhirService;
    private final FhirToHl7Service fhirToHl7Service;
    private final ExecutorService executorService;

    @Value("${app.batch.max-size:100}")
    private int maxBatchSize;

    @Autowired
    public BatchConversionService(Hl7ToFhirService hl7ToFhirService,
            FhirToHl7Service fhirToHl7Service) {
        this.hl7ToFhirService = hl7ToFhirService;
        this.fhirToHl7Service = fhirToHl7Service;
        // Java 21 Virtual Threads: lightweight, scalable threading for I/O-bound tasks
        // Each conversion task runs on a virtual thread with minimal overhead
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        log.info("BatchConversionService initialized with Java 21 virtual threads");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down BatchConversionService executor...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                log.warn("BatchConversionService executor forced shutdown after 30s timeout");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void validateBatchSize(int size) {
        if (size == 0) {
            throw new IllegalArgumentException("Batch must contain at least one message");
        }
        if (size > maxBatchSize) {
            throw new IllegalArgumentException(
                    String.format("Batch size %d exceeds maximum allowed size of %d", size, maxBatchSize));
        }
    }

    /**
     * Process a batch of HL7 messages.
     * 
     * @param request  Batch request containing messages
     * @param tenantId Tenant ID
     * @return BatchConversionResponse with results and errors
     */
    public BatchConversionResponse processBatch(BatchHl7Request request, String tenantId) {
        log.info("Processing batch request for tenant: {}", tenantId);
        return convertHl7ToFhirBatch(request.getMessages());
    }

    /**
     * Convert multiple HL7 messages to FHIR in parallel.
     * 
     * @param hl7Messages List of HL7 v2.5 messages
     * @return BatchConversionResponse with results and errors
     */
    public BatchConversionResponse convertHl7ToFhirBatch(List<String> hl7Messages) {
        validateBatchSize(hl7Messages.size());
        long startTime = System.currentTimeMillis();
        log.info("Starting batch HL7 to FHIR conversion: {} messages", hl7Messages.size());

        BatchConversionResponse response = new BatchConversionResponse();
        response.setTotalMessages(hl7Messages.size());
        response.setResults(new ArrayList<>());
        response.setErrors(new ArrayList<>());

        // Create futures for parallel processing
        List<CompletableFuture<BatchItemResult>> futures = new ArrayList<>();

        for (int i = 0; i < hl7Messages.size(); i++) {
            final int index = i;
            final String hl7Message = hl7Messages.get(i);

            CompletableFuture<BatchItemResult> future = CompletableFuture.supplyAsync(() -> {
                long msgStartTime = System.currentTimeMillis();
                try {
                    String fhirJson = hl7ToFhirService.convertHl7ToFhir(hl7Message);
                    long msgEndTime = System.currentTimeMillis();

                    // Extract message ID from result if possible
                    String messageId = extractMessageId(fhirJson);

                    return new BatchConversionResponse.BatchItemResult(
                            index,
                            fhirJson,
                            msgEndTime - msgStartTime,
                            messageId);
                } catch (Exception e) {
                    log.error("Failed to convert message at index {}", index, e);
                    // Return null to indicate failure
                    return null;
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all conversions to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Collect results and errors
        for (int i = 0; i < futures.size(); i++) {
            try {
                BatchItemResult result = futures.get(i).get();
                if (result != null) {
                    response.getResults().add(result);
                    response.setSuccessCount(response.getSuccessCount() + 1);
                } else {
                    // Conversion failed
                    String truncatedInput = truncate(hl7Messages.get(i), 200);
                    response.getErrors().add(new BatchConversionResponse.BatchConversionErrorDetails(
                            i,
                            "Conversion failed - see logs for details",
                            truncatedInput));
                    response.setFailureCount(response.getFailureCount() + 1);
                }
            } catch (Exception e) {
                String truncatedInput = truncate(hl7Messages.get(i), 200);
                response.getErrors().add(new BatchConversionResponse.BatchConversionErrorDetails(
                        i,
                        e.getMessage(),
                        truncatedInput));
                response.setFailureCount(response.getFailureCount() + 1);
            }
        }

        long endTime = System.currentTimeMillis();
        response.setProcessingTimeMs(endTime - startTime);

        log.info("Batch HL7 to FHIR conversion completed: {} success, {} failures, {}ms total",
                response.getSuccessCount(), response.getFailureCount(), response.getProcessingTimeMs());

        return response;
    }

    /**
     * Convert multiple FHIR bundles to HL7 in parallel.
     * 
     * @param fhirBundles List of FHIR Bundle JSON strings
     * @return BatchConversionResponse with results and errors
     */
    public BatchConversionResponse convertFhirToHl7Batch(List<String> fhirBundles) {
        validateBatchSize(fhirBundles.size());
        long startTime = System.currentTimeMillis();
        log.info("Starting batch FHIR to HL7 conversion: {} bundles", fhirBundles.size());

        BatchConversionResponse response = new BatchConversionResponse();
        response.setTotalMessages(fhirBundles.size());
        response.setResults(new ArrayList<>());
        response.setErrors(new ArrayList<>());

        // Create futures for parallel processing
        List<CompletableFuture<BatchItemResult>> futures = new ArrayList<>();

        for (int i = 0; i < fhirBundles.size(); i++) {
            final int index = i;
            final String fhirBundle = fhirBundles.get(i);

            CompletableFuture<BatchItemResult> future = CompletableFuture.supplyAsync(() -> {
                long msgStartTime = System.currentTimeMillis();
                try {
                    String hl7Message = fhirToHl7Service.convertFhirToHl7(fhirBundle);
                    long msgEndTime = System.currentTimeMillis();

                    // Extract message ID from HL7 (MSH-10)
                    String messageId = extractHl7MessageId(hl7Message);

                    return new BatchConversionResponse.BatchItemResult(
                            index,
                            hl7Message,
                            msgEndTime - msgStartTime,
                            messageId);
                } catch (Exception e) {
                    log.error("Failed to convert bundle at index {}", index, e);
                    // Return null to indicate failure, error details will be collected later
                    return null;
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all conversions to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Collect results and errors
        for (int i = 0; i < futures.size(); i++) {
            try {
                BatchItemResult result = futures.get(i).get();
                if (result != null) {
                    response.getResults().add(result);
                    response.setSuccessCount(response.getSuccessCount() + 1);
                } else {
                    String truncatedInput = truncate(fhirBundles.get(i), 200);
                    response.getErrors().add(new BatchConversionResponse.BatchConversionErrorDetails(
                            i,
                            "Conversion failed - see logs for details",
                            truncatedInput));
                    response.setFailureCount(response.getFailureCount() + 1);
                }
            } catch (Exception e) {
                String truncatedInput = truncate(fhirBundles.get(i), 200);
                response.getErrors().add(new BatchConversionResponse.BatchConversionErrorDetails(
                        i,
                        e.getMessage(),
                        truncatedInput));
                response.setFailureCount(response.getFailureCount() + 1);
            }
        }

        long endTime = System.currentTimeMillis();
        response.setProcessingTimeMs(endTime - startTime);

        log.info("Batch FHIR to HL7 conversion completed: {} success, {} failures, {}ms total",
                response.getSuccessCount(), response.getFailureCount(), response.getProcessingTimeMs());

        return response;
    }

    /**
     * Extract message ID from FHIR Bundle JSON.
     */
    private String extractMessageId(String fhirJson) {
        try {
            // Simple extraction - look for "id" field
            int idIndex = fhirJson.indexOf("\"id\"");
            if (idIndex > 0) {
                int valueStart = fhirJson.indexOf("\"", idIndex + 5) + 1;
                int valueEnd = fhirJson.indexOf("\"", valueStart);
                return fhirJson.substring(valueStart, valueEnd);
            }
        } catch (Exception e) {
            log.debug("Could not extract message ID from FHIR: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * Extract message ID from HL7 message (MSH-10).
     */
    private String extractHl7MessageId(String hl7Message) {
        try {
            // MSH-10 is the 10th field in MSH segment
            String[] segments = hl7Message.split("\r");
            if (segments.length > 0 && segments[0].startsWith("MSH")) {
                String[] fields = segments[0].split("\\|");
                if (fields.length >= 10) {
                    return fields[9]; // MSH-10 (0-indexed, so field 9)
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract message ID from HL7: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * Truncate string to specified length.
     */
    private String truncate(String str, int maxLength) {
        if (str == null)
            return null;
        if (str.length() <= maxLength)
            return str;
        return str.substring(0, maxLength) + "...";
    }
}

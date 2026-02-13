package com.al.hl7fhirtransformer.service;

import com.al.hl7fhirtransformer.model.TransactionRecord;
import com.al.hl7fhirtransformer.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IdempotencyService {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final TransactionRepository transactionRepository;

    @Autowired
    public IdempotencyService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Check if a request with the given idempotency key has already been processed.
     *
     * @param idempotencyKey The client-provided idempotency key
     * @return Optional containing the existing transaction record if found
     */
    public Optional<TransactionRecord> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        Optional<TransactionRecord> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            log.info("Found existing transaction for idempotency key: {}", idempotencyKey);
        }

        return existing;
    }

    /**
     * Validate idempotency key format.
     *
     * @param idempotencyKey The key to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }

        // RFC 7231: Idempotency key should be ASCII, max 255 characters
        if (idempotencyKey.length() > 255) {
            log.warn("Idempotency key exceeds 255 characters: {}", idempotencyKey.length());
            return false;
        }

        return true;
    }

    /**
     * Check if a request with the given idempotency key is a duplicate.
     * 
     * @param idempotencyKey The client-provided idempotency key
     * @return true if duplicate, false otherwise
     */
    public boolean isDuplicate(String idempotencyKey) {
        return findByIdempotencyKey(idempotencyKey).isPresent();
    }
}

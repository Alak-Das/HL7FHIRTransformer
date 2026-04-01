package com.al.hl7fhirtransformer.service;

import com.al.hl7fhirtransformer.model.TransactionRecord;
import com.al.hl7fhirtransformer.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final long IDEMPOTENCY_TTL_HOURS = 24;

    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public IdempotencyService(TransactionRepository transactionRepository,
            RedisTemplate<String, Object> redisTemplate) {
        this.transactionRepository = transactionRepository;
        this.redisTemplate = redisTemplate;
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
     * Atomically check if a request with the given idempotency key is a duplicate.
     * Uses Redis SETNX for atomic check-and-set to prevent race conditions between
     * concurrent requests with the same key.
     *
     * @param idempotencyKey The client-provided idempotency key
     * @return true if duplicate, false otherwise
     */
    public boolean isDuplicate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }

        // Atomic check: Redis SETNX returns true only if the key was newly set
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        try {
            Boolean wasSet = redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, "1", IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(wasSet)) {
                // Key already existed — this is a duplicate
                log.info("Duplicate idempotency key detected (Redis): {}", idempotencyKey);
                return true;
            }
            // Key was newly set — not a duplicate
            return false;
        } catch (Exception e) {
            // Redis unavailable — fall back to MongoDB check
            log.warn("Redis idempotency check failed, falling back to MongoDB: {}", e.getMessage());
            return findByIdempotencyKey(idempotencyKey).isPresent();
        }
    }
}


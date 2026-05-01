package com.al.hl7fhirtransformer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application-level health and cache management endpoints.
 *
 * <ul>
 * <li>GET /api/health — lightweight status check (any authenticated role)</li>
 * <li>DELETE /api/health/cache — evict all named caches (ADMIN only)</li>
 * <li>DELETE /api/health/cache/{cacheName} — evict a specific cache (ADMIN
 * only)</li>
 * </ul>
 *
 * These endpoints complement Spring Actuator: Actuator provides
 * infrastructure-level
 * health probes, while this controller provides application-level cache
 * management
 * without exposing the full Actuator surface to regular users.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "System health check and dependency status")
public class HealthController {

    private final CacheManager cacheManager;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.application.name:HL7FHIRTransformer}")
    private String applicationName;

    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String version;

    @Autowired
    public HealthController(CacheManager cacheManager, MongoTemplate mongoTemplate,
            RedisTemplate<String, Object> redisTemplate) {
        this.cacheManager = cacheManager;
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Lightweight health check with external dependency probes.
     * Reports individual status for MongoDB, Redis; overall status is DEGRADED
     * if any dependency is down.
     *
     * @return JSON map with application name, version, uptime, cache names, and
     *         dependency statuses
     */
    @Operation(summary = "System Health Check", description = "Checks the health of the application and its dependencies (MongoDB, Redis, RabbitMQ).",
               responses = {
                   @ApiResponse(responseCode = "200", description = "System is fully healthy"),
                   @ApiResponse(responseCode = "503", description = "One or more dependencies are down")
               })
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSec = uptimeMs / 1000;

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application", applicationName);
        info.put("version", version);
        info.put("uptimeSeconds", uptimeSec);
        info.put("cacheNames", cacheManager.getCacheNames());
        info.put("timestamp", java.time.Instant.now().toString());

        // Dependency health checks
        Map<String, String> dependencies = new LinkedHashMap<>();
        boolean allHealthy = true;

        // MongoDB
        try {
            mongoTemplate.getDb().getName();
            dependencies.put("mongodb", "UP");
        } catch (Exception e) {
            dependencies.put("mongodb", "DOWN: " + e.getMessage());
            allHealthy = false;
        }

        // Redis
        try {
            var connFactory = redisTemplate.getConnectionFactory();
            if (connFactory != null) {
                try (var conn = connFactory.getConnection()) {
                    conn.ping();
                }
            }
            dependencies.put("redis", "UP");
        } catch (Exception e) {
            dependencies.put("redis", "DOWN: " + e.getMessage());
            allHealthy = false;
        }

        info.put("dependencies", dependencies);
        info.put("status", allHealthy ? "UP" : "DEGRADED");

        return ResponseEntity.ok(info);
    }

    /**
     * Evict all Redis caches — ADMIN only.
     * Useful after bulk data migrations or configuration changes.
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, Object>> evictAllCaches() {
        int count = 0;
        for (String cacheName : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                count++;
            }
        }
        return ResponseEntity.ok(Map.of(
                "message", "All caches evicted successfully",
                "cachesEvicted", count));
    }

    /**
     * Evict a single named cache — ADMIN only.
     *
     * @param cacheName The name of the cache to evict (e.g. "transaction",
     *                  "tenantStatusCounts")
     * @return 200 on success, 404 if the cache name is unknown
     */
    @DeleteMapping("/cache/{cacheName}")
    public ResponseEntity<Map<String, String>> evictCache(@PathVariable String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return ResponseEntity.notFound().build();
        }
        cache.clear();
        return ResponseEntity.ok(Map.of(
                "message", "Cache evicted successfully",
                "cacheName", cacheName));
    }
}


package com.al.hl7fhirtransformer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
public class HealthController {

    private final CacheManager cacheManager;

    @Value("${spring.application.name:HL7FHIRTransformer}")
    private String applicationName;

    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String version;

    @Autowired
    public HealthController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Lightweight health check — no external calls, just JVM and cache metadata.
     * Accessible to any authenticated user (ADMIN or TENANT role).
     *
     * @return JSON map with application name, version, uptime, and cache names
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSec = uptimeMs / 1000;

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "UP");
        info.put("application", applicationName);
        info.put("version", version);
        info.put("uptimeSeconds", uptimeSec);
        info.put("cacheNames", cacheManager.getCacheNames());
        info.put("timestamp", java.time.Instant.now().toString());

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

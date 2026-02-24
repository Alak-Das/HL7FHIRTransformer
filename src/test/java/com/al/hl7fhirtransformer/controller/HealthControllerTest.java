package com.al.hl7fhirtransformer.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private HealthController controller;

    @Test
    void health_shouldReturnStatusUpWithMetadata() {
        Collection<String> cacheNames = Arrays.asList("transaction", "tenantStatusCounts");
        when(cacheManager.getCacheNames()).thenReturn(cacheNames);

        // Inject @Value fields that aren't set by Spring in unit tests
        ReflectionTestUtils.setField(controller, "applicationName", "HL7FHIRTransformer");
        ReflectionTestUtils.setField(controller, "version", "0.0.1-SNAPSHOT");

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("HL7FHIRTransformer", response.getBody().get("application"));
        assertEquals("0.0.1-SNAPSHOT", response.getBody().get("version"));
        assertTrue(response.getBody().containsKey("uptimeSeconds"));
        assertTrue(response.getBody().containsKey("timestamp"));
        assertEquals(cacheNames, response.getBody().get("cacheNames"));
    }

    @Test
    void evictAllCaches_shouldClearAllCachesAndReturnCount() {
        Cache txCache = mock(Cache.class);
        Cache statsCache = mock(Cache.class);

        when(cacheManager.getCacheNames()).thenReturn(List.of("transaction", "tenantStatusCounts"));
        when(cacheManager.getCache("transaction")).thenReturn(txCache);
        when(cacheManager.getCache("tenantStatusCounts")).thenReturn(statsCache);

        ResponseEntity<Map<String, Object>> response = controller.evictAllCaches();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().get("cachesEvicted"));
        verify(txCache).clear();
        verify(statsCache).clear();
    }

    @Test
    void evictAllCaches_whenNoCaches_shouldReturn0Count() {
        when(cacheManager.getCacheNames()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.evictAllCaches();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().get("cachesEvicted"));
    }

    @Test
    void evictCache_whenCacheExists_shouldClearAndReturn200() {
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache("transaction")).thenReturn(mockCache);

        ResponseEntity<Map<String, String>> response = controller.evictCache("transaction");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("transaction", response.getBody().get("cacheName"));
        assertTrue(response.getBody().get("message").contains("evicted"));
        verify(mockCache).clear();
    }

    @Test
    void evictCache_whenCacheNotFound_shouldReturn404() {
        when(cacheManager.getCache("nonexistent")).thenReturn(null);

        ResponseEntity<Map<String, String>> response = controller.evictCache("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

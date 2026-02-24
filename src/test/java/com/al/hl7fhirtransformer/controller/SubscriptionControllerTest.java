package com.al.hl7fhirtransformer.controller;

import com.al.hl7fhirtransformer.model.SubscriptionEntity;
import com.al.hl7fhirtransformer.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionController controller;

    private SubscriptionEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new SubscriptionEntity();
        testEntity.setId("sub-123");
        testEntity.setTenantId("tenant1");
        testEntity.setCriteria("Patient");
        testEntity.setEndpoint("http://webhook.example.com/hook");
        testEntity.setStatus("active");
        testEntity.setChannelType("rest-hook");
        testEntity.setCreatedDate(LocalDateTime.now());
        testEntity.setLastUpdatedDate(LocalDateTime.now());
    }

    @Test
    void createSubscription_shouldDelegate_andReturn200() {
        when(subscriptionService.createSubscription("tenant1", "Patient", "http://hook")).thenReturn(testEntity);

        ResponseEntity<SubscriptionEntity> response = controller.createSubscription("Patient", "http://hook",
                "tenant1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("sub-123", response.getBody().getId());
        verify(subscriptionService).createSubscription("tenant1", "Patient", "http://hook");
    }

    @Test
    void createSubscription_withNullTenantId_shouldUseDefault() {
        when(subscriptionService.createSubscription("default", "Patient", "http://hook")).thenReturn(testEntity);

        ResponseEntity<SubscriptionEntity> response = controller.createSubscription("Patient", "http://hook", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(subscriptionService).createSubscription("default", "Patient", "http://hook");
    }

    @Test
    void getSubscriptions_shouldReturnList() {
        when(subscriptionService.getActiveSubscriptions("tenant1")).thenReturn(List.of(testEntity));

        ResponseEntity<List<SubscriptionEntity>> response = controller.getSubscriptions("tenant1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void updateSubscription_whenFound_shouldReturn200() {
        SubscriptionEntity updated = new SubscriptionEntity();
        updated.setId("sub-123");
        updated.setCriteria("Patient?gender=female");
        updated.setEndpoint("http://new-hook.example.com/hook");
        updated.setStatus("active");

        when(subscriptionService.updateSubscription("sub-123", "Patient?gender=female",
                "http://new-hook.example.com/hook", "tenant1")).thenReturn(updated);

        ResponseEntity<SubscriptionEntity> response = controller.updateSubscription(
                "sub-123", "Patient?gender=female", "http://new-hook.example.com/hook", "tenant1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Patient?gender=female", response.getBody().getCriteria());
    }

    @Test
    void updateSubscription_whenNotFound_shouldReturn404() {
        when(subscriptionService.updateSubscription(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Not found"));

        ResponseEntity<SubscriptionEntity> response = controller.updateSubscription(
                "nonexistent", "Patient", "http://hook", "tenant1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void cancelSubscription_whenFound_shouldReturn200WithMessage() {
        doNothing().when(subscriptionService).cancelSubscription("sub-123", "tenant1");

        ResponseEntity<?> response = controller.cancelSubscription("sub-123", "tenant1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        var body = (java.util.Map<String, String>) response.getBody();
        assertEquals("sub-123", body.get("id"));
        assertTrue(body.get("message").contains("cancelled"));
        verify(subscriptionService).cancelSubscription("sub-123", "tenant1");
    }

    @Test
    void cancelSubscription_whenNotFound_shouldReturn404() {
        doThrow(new IllegalArgumentException("Not found"))
                .when(subscriptionService).cancelSubscription("nonexistent", "tenant1");

        ResponseEntity<?> response = controller.cancelSubscription("nonexistent", "tenant1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

package com.al.hl7fhirtransformer.repository;

import com.al.hl7fhirtransformer.model.SubscriptionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends MongoRepository<SubscriptionEntity, String> {
    List<SubscriptionEntity> findByTenantIdAndStatus(String tenantId, String status);

    Optional<SubscriptionEntity> findByIdAndTenantId(String id, String tenantId);
}

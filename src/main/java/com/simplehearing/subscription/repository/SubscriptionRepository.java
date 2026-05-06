package com.simplehearing.subscription.repository;

import com.simplehearing.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /** All subscriptions for a patient within the org, newest first */
    List<Subscription> findByOrgIdAndPatientIdOrderByCreatedAtDesc(UUID orgId, UUID patientId);
}

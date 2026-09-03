package com.simplehearing.subscription.repository;

import com.simplehearing.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /** All subscriptions for a patient within the org, newest first */
    List<Subscription> findByOrgIdAndPatientIdOrderByCreatedAtDesc(UUID orgId, UUID patientId);

    /** Bulk variant — avoids one query per patient when building a page of Cases. */
    List<Subscription> findByOrgIdAndPatientIdInOrderByCreatedAtDesc(UUID orgId, Collection<UUID> patientIds);

    /** Every subscription in the org — used for org-wide rollups (e.g. per-patient payment status). */
    List<Subscription> findByOrgId(UUID orgId);

    void deleteByPatientId(UUID patientId);
}

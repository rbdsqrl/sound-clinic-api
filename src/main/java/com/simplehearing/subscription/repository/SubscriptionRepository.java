package com.simplehearing.subscription.repository;

import com.simplehearing.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    /** Subscriptions still owed money whose next payment-reminder is due — either never
     *  reminded and created at least the cadence ago, or last reminded at least that long ago.
     *  Excludes PAID (nothing left to remind about) and non-ACTIVE (e.g. CANCELLED) automatically,
     *  so a subscription naturally stops appearing here the moment it's paid off or cancelled —
     *  see PaymentReminderService. */
    @Query("SELECT s FROM Subscription s WHERE s.status = com.simplehearing.subscription.enums.SubscriptionStatus.ACTIVE "
         + "AND s.paymentStatus <> com.simplehearing.subscription.enums.SubscriptionPaymentStatus.PAID "
         + "AND ((s.paymentReminderSentAt IS NULL AND s.createdAt <= :cutoff) OR s.paymentReminderSentAt <= :cutoff)")
    List<Subscription> findDueForPaymentReminder(@Param("cutoff") Instant cutoff);

    void deleteByPatientId(UUID patientId);

    /** (patientId, paymentStatus) of each given patient's most recently created subscription. */
    @Query("SELECT s.patientId, s.paymentStatus FROM Subscription s WHERE s.orgId = :orgId AND s.patientId IN :patientIds "
         + "AND s.createdAt = (SELECT MAX(s2.createdAt) FROM Subscription s2 WHERE s2.orgId = :orgId AND s2.patientId = s.patientId)")
    List<Object[]> findLatestPaymentStatusByPatient(@Param("orgId") UUID orgId,
                                                    @Param("patientIds") Collection<UUID> patientIds);
}

package com.simplehearing.iep.repository;

import com.simplehearing.iep.entity.IEPPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IEPPlanRepository extends JpaRepository<IEPPlan, UUID> {

    List<IEPPlan> findByOrgIdAndPatientIdOrderByCreatedAtDesc(UUID orgId, UUID patientId);

    List<IEPPlan> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    /** Per-therapist count of plans created in [from, to) — the Members analytics tab's "IEP Created"
     *  column, without loading every plan in the org just to tally therapistId. */
    @Query("SELECT p.therapistId, COUNT(p) FROM IEPPlan p "
         + "WHERE p.orgId = :orgId AND p.therapistId IS NOT NULL AND p.createdAt >= :from AND p.createdAt < :to "
         + "GROUP BY p.therapistId")
    List<Object[]> countCreatedByTherapist(@Param("orgId") UUID orgId,
                                           @Param("from") Instant from,
                                           @Param("to") Instant to);

    Optional<IEPPlan> findByIdAndOrgId(UUID id, UUID orgId);

    List<IEPPlan> findByPatientId(UUID patientId);

    List<IEPPlan> findByOrgIdAndTherapistId(UUID orgId, UUID therapistId);

    /** Plans belonging to a specific program — used for goal-mastery-per-enrollment. */
    List<IEPPlan> findByEnrollmentId(UUID enrollmentId);

    /** Plans currently owned by a specific bulk therapist reassignment — the revert scan. */
    List<IEPPlan> findByReassignmentId(UUID reassignmentId);

    void deleteByPatientId(UUID patientId);

    /** Plans for a set of patients in one round trip (the batched Cases trend endpoint). */
    List<IEPPlan> findByOrgIdAndPatientIdIn(UUID orgId, java.util.Collection<UUID> patientIds);
}

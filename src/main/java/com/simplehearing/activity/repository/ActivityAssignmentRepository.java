package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityAssignmentRepository extends JpaRepository<ActivityAssignment, UUID> {
    List<ActivityAssignment> findByOrgIdAndPatientIdOrderByCreatedAtDesc(UUID orgId, UUID patientId);
    Optional<ActivityAssignment> findByIdAndOrgId(UUID id, UUID orgId);
    long countByOrgIdAndPatientId(UUID orgId, UUID patientId);
    List<ActivityAssignment> findByOrgId(UUID orgId);

    /** Per-assigner count of assignments created in [from, to) — the Members analytics tab's
     *  "Activities Assigned" column, computed in SQL instead of loading every assignment ever made. */
    @Query("SELECT aa.assignedBy, COUNT(aa) FROM ActivityAssignment aa "
         + "WHERE aa.orgId = :orgId AND aa.createdAt >= :from AND aa.createdAt < :to GROUP BY aa.assignedBy")
    List<Object[]> countAssignedByAssigner(@Param("orgId") UUID orgId,
                                           @Param("from") Instant from,
                                           @Param("to") Instant to);

    /** Per-patient count of every assignment in the org — the Cases tab's "Activities" column. */
    @Query("SELECT aa.patientId, COUNT(aa) FROM ActivityAssignment aa WHERE aa.orgId = :orgId GROUP BY aa.patientId")
    List<Object[]> countByPatient(@Param("orgId") UUID orgId);

    /** Most-assigned activities (activityId, count), highest first — pass a PageRequest to cap it. */
    @Query("SELECT aa.activityId, COUNT(aa) FROM ActivityAssignment aa WHERE aa.orgId = :orgId "
         + "GROUP BY aa.activityId ORDER BY COUNT(aa) DESC")
    List<Object[]> countByActivity(@Param("orgId") UUID orgId, org.springframework.data.domain.Pageable pageable);
}

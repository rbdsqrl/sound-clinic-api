package com.simplehearing.evidence.repository;

import com.simplehearing.evidence.entity.GoalEvidence;
import com.simplehearing.evidence.enums.EvidenceKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalEvidenceRepository extends JpaRepository<GoalEvidence, UUID> {

    List<GoalEvidence> findByOrgIdAndPatientIdOrderByCreatedAtDesc(UUID orgId, UUID patientId);

    List<GoalEvidence> findByOrgIdAndPatientIdAndGoalIdOrderByCreatedAtDesc(UUID orgId, UUID patientId, UUID goalId);

    Optional<GoalEvidence> findByIdAndOrgIdAndPatientId(UUID id, UUID orgId, UUID patientId);

    List<GoalEvidence> findByGoalIdIn(Collection<UUID> goalIds);

    long countByGoalIdAndKind(UUID goalId, EvidenceKind kind);

    boolean existsByGoalIdAndKind(UUID goalId, EvidenceKind kind);

    /** (goalId, kind, count) for a set of goals — evidence status of a batch of completed goals. */
    @Query("SELECT e.goalId, e.kind, COUNT(e) FROM GoalEvidence e WHERE e.goalId IN :goalIds GROUP BY e.goalId, e.kind")
    List<Object[]> countByGoalAndKind(@Param("goalIds") Collection<UUID> goalIds);

    /** (therapistId, count) of videos uploaded in [from, to). */
    @Query("SELECT e.therapistId, COUNT(e) FROM GoalEvidence e WHERE e.orgId = :orgId AND e.kind = com.simplehearing.evidence.enums.EvidenceKind.VIDEO "
         + "AND e.createdAt >= :from AND e.createdAt < :to GROUP BY e.therapistId")
    List<Object[]> countVideosByTherapist(@Param("orgId") UUID orgId, @Param("from") Instant from, @Param("to") Instant to);

    /** (therapistId, reasonCode, count) of "couldn't upload" records in [from, to). */
    @Query("SELECT e.therapistId, e.reasonCode, COUNT(e) FROM GoalEvidence e WHERE e.orgId = :orgId AND e.kind = com.simplehearing.evidence.enums.EvidenceKind.CANNOT_UPLOAD "
         + "AND e.createdAt >= :from AND e.createdAt < :to GROUP BY e.therapistId, e.reasonCode")
    List<Object[]> countCannotUploadByTherapistAndReason(@Param("orgId") UUID orgId, @Param("from") Instant from, @Param("to") Instant to);

    /** (patientId, count) of videos uploaded in [from, to). */
    @Query("SELECT e.patientId, COUNT(e) FROM GoalEvidence e WHERE e.orgId = :orgId AND e.kind = com.simplehearing.evidence.enums.EvidenceKind.VIDEO "
         + "AND e.createdAt >= :from AND e.createdAt < :to GROUP BY e.patientId")
    List<Object[]> countVideosByPatient(@Param("orgId") UUID orgId, @Param("from") Instant from, @Param("to") Instant to);
}

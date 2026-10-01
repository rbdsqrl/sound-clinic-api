package com.simplehearing.iep.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import com.simplehearing.iep.entity.IEPGoal;
import com.simplehearing.iep.enums.IEPGoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IEPGoalRepository extends JpaRepository<IEPGoal, UUID> {

    List<IEPGoal> findByPlanIdOrderByCreatedAtAsc(UUID planId);

    Optional<IEPGoal> findByIdAndOrgId(UUID id, UUID orgId);

    int countByPlanIdAndStatus(UUID planId, IEPGoalStatus status);

    void deleteByPlanId(UUID planId);

    List<IEPGoal> findByPlanId(UUID planId);

    /** Goals across several plans at once — avoids an N+1 when building analytics series. */
    List<IEPGoal> findByPlanIdIn(List<UUID> planIds);

    List<IEPGoal> findByOrgId(UUID orgId);

    /** Per-patient goal count across every plan in the org — the Cases tab's "LT Goals" column. */
    @Query("SELECT p.patientId, COUNT(g) FROM IEPGoal g, IEPPlan p WHERE g.planId = p.id AND g.orgId = :orgId AND p.orgId = :orgId GROUP BY p.patientId")
    List<Object[]> countByPatient(@Param("orgId") UUID orgId);
}

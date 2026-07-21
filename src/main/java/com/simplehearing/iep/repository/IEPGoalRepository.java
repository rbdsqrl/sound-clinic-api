package com.simplehearing.iep.repository;

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
}

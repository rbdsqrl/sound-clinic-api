package com.simplehearing.iep.repository;

import com.simplehearing.iep.entity.IEPGoalProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface IEPGoalProgressRepository extends JpaRepository<IEPGoalProgress, UUID> {

    List<IEPGoalProgress> findByGoalIdOrderBySessionDateDesc(UUID goalId);

    int countByGoalId(UUID goalId);

    void deleteByGoalId(UUID goalId);

    /** Every progress entry in an org inside a date window — the org-wide analytics scan. */
    List<IEPGoalProgress> findByOrgIdAndSessionDateBetween(UUID orgId, LocalDate from, LocalDate to);

    /** Progress entries for a known set of goals inside a date window (patient / therapist scope). */
    List<IEPGoalProgress> findByGoalIdInAndSessionDateBetween(
            List<UUID> goalIds, LocalDate from, LocalDate to);

    /** Every progress entry ever logged for a set of goals — the life-of-enrollment total for goal mastery. */
    List<IEPGoalProgress> findByGoalIdIn(List<UUID> goalIds);
}

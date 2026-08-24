package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityAttemptLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ActivityAttemptLogRepository extends JpaRepository<ActivityAttemptLog, UUID> {
    List<ActivityAttemptLog> findByAssignmentIdOrderByAttemptDateDesc(UUID assignmentId);

    List<ActivityAttemptLog> findByOrgIdAndAssignmentIdInAndAttemptDateBetween(
            UUID orgId, List<UUID> assignmentIds, LocalDate from, LocalDate to);
}

package com.simplehearing.iep.dto;

import com.simplehearing.iep.entity.IEPGoal;
import com.simplehearing.iep.entity.IEPGoalProgress;
import com.simplehearing.iep.enums.IEPGoalDomain;
import com.simplehearing.iep.enums.IEPGoalStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record IEPGoalResponse(
        UUID id,
        UUID orgId,
        UUID planId,
        String title,
        String goalStatement,
        IEPGoalDomain domain,
        String baseline,
        String targetCriteria,
        String targetDate,
        IEPGoalStatus status,
        String progressTag,
        UUID assignedTherapistId,
        String therapistName,
        int progressCount,
        /** Trials passed ÷ trials attempted on the most recent progress entry — null when that
         *  entry didn't record trial counts, or none has been logged yet. */
        Integer latestMasteryPct,
        Instant createdAt,
        Instant updatedAt
) {
    /** {@code progress} need not be pre-sorted — the most recent entry is found by session date. */
    public static IEPGoalResponse from(IEPGoal goal, String therapistName, List<IEPGoalProgress> progress) {
        Integer latestMasteryPct = progress.stream()
                .max(Comparator.comparing(IEPGoalProgress::getSessionDate)
                        .thenComparing(IEPGoalProgress::getCreatedAt))
                .map(p -> masteryPct(p.getTrialsPassed(), p.getTrialsTotal()))
                .orElse(null);

        return new IEPGoalResponse(
                goal.getId(),
                goal.getOrgId(),
                goal.getPlanId(),
                goal.getTitle(),
                goal.getGoalStatement(),
                goal.getDomain(),
                goal.getBaseline(),
                goal.getTargetCriteria(),
                goal.getTargetDate() != null ? goal.getTargetDate().toString() : null,
                goal.getStatus(),
                goal.getProgressTag(),
                goal.getAssignedTherapistId(),
                therapistName,
                progress.size(),
                latestMasteryPct,
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }

    public static Integer masteryPct(Integer trialsPassed, Integer trialsTotal) {
        if (trialsPassed == null || trialsTotal == null || trialsTotal <= 0) return null;
        return (int) Math.round(100.0 * trialsPassed / trialsTotal);
    }
}

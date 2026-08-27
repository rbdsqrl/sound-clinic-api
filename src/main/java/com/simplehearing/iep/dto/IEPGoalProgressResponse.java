package com.simplehearing.iep.dto;

import com.simplehearing.iep.entity.IEPGoalProgress;

import java.time.Instant;
import java.util.UUID;

public record IEPGoalProgressResponse(
        UUID id,
        String sessionDate,
        String note,
        Integer trialsPassed,
        Integer trialsTotal,
        Integer masteryPct,
        String therapistName,
        Instant createdAt
) {
    public static IEPGoalProgressResponse from(IEPGoalProgress p, String therapistName) {
        return new IEPGoalProgressResponse(
                p.getId(),
                p.getSessionDate().toString(),
                p.getNote(),
                p.getTrialsPassed(),
                p.getTrialsTotal(),
                IEPGoalResponse.masteryPct(p.getTrialsPassed(), p.getTrialsTotal()),
                therapistName,
                p.getCreatedAt()
        );
    }
}

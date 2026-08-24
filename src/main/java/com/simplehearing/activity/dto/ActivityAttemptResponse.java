package com.simplehearing.activity.dto;

import com.simplehearing.activity.entity.ActivityAttemptLog;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ActivityAttemptResponse(
        UUID id,
        UUID assignmentId,
        UUID loggedBy,
        String loggedByName,
        LocalDate attemptDate,
        String note,
        List<AttemptAnswerResponse> answers,
        Instant createdAt
) {
    public static ActivityAttemptResponse from(ActivityAttemptLog log, String loggedByName, List<AttemptAnswerResponse> answers) {
        return new ActivityAttemptResponse(
                log.getId(), log.getAssignmentId(), log.getLoggedBy(), loggedByName,
                log.getAttemptDate(), log.getNote(), answers, log.getCreatedAt());
    }
}

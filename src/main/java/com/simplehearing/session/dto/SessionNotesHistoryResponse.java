package com.simplehearing.session.dto;

import com.simplehearing.session.entity.SessionNotesHistory;

import java.time.Instant;
import java.util.UUID;

/** One prior version of a session's feedback/progress report/notes/performance score,
 *  captured right before it was overwritten by a later edit. */
public record SessionNotesHistoryResponse(
        UUID id,
        UUID sessionId,
        UUID changedBy,
        String changedByName,
        Instant changedAt,
        String previousFeedback,
        String previousProgressReport,
        String previousNotes,
        Integer previousPerformanceScore
) {
    public static SessionNotesHistoryResponse from(SessionNotesHistory h, String changedByName) {
        return new SessionNotesHistoryResponse(
                h.getId(),
                h.getSessionId(),
                h.getChangedBy(),
                changedByName,
                h.getChangedAt(),
                h.getPreviousFeedback(),
                h.getPreviousProgressReport(),
                h.getPreviousNotes(),
                h.getPreviousPerformanceScore());
    }
}

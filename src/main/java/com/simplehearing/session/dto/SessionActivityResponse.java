package com.simplehearing.session.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One entry in a session's Activity Log. {@code legacy} entries predate the event log: they only
 * know what notes held <i>before</i> an edit, so their changes carry a {@code from} and no {@code to}.
 */
public record SessionActivityResponse(
        UUID id,
        UUID actorId,
        String actorName,
        String type,
        String summary,
        List<Change> changes,
        boolean legacy,
        Instant createdAt
) {
    /** A before/after pair, already formatted for display; null means "not set". */
    public record Change(String field, String from, String to) {}
}

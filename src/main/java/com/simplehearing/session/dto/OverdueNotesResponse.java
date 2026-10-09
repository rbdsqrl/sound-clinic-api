package com.simplehearing.session.dto;

import java.util.List;

/**
 * A therapist's sessions that have ended but are still SCHEDULED — nobody marked them or wrote
 * them up. {@code count} is the full total; {@code sessions} is as many of them as were asked for
 * (oldest first), so the dashboard can show a preview without loading the rest.
 */
public record OverdueNotesResponse(
        int count,
        List<TherapySessionSummaryResponse> sessions
) {}

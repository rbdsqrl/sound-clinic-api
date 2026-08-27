package com.simplehearing.analytics.dto;

import java.util.UUID;

/**
 * One row per patient for the "Cases" analytics tab — a Kidaura-parity list view.
 * {@code sessionsAttended}/{@code sessionsCancelled} are scoped to the requested [from, to]
 * window; {@code sessionsUpcoming} counts scheduled sessions from today onward regardless of
 * the window's "to" date, so a narrow past window still shows what's next.
 */
public record CaseSummaryResponse(
        UUID patientId,
        String patientName,
        int sessionsAttended,
        int sessionsUpcoming,
        int sessionsCancelled,
        int membersAssigned,
        int activitiesAssigned,
        int checklistFilled,
        int ltGoals,
        String paymentStatus
) {}

package com.simplehearing.analytics.dto;

import java.util.UUID;

/**
 * One row per therapist for the "Members" analytics tab — a Kidaura-parity list view.
 * {@code sessionsCancelled} and {@code activitiesAssigned} are scoped to the requested
 * [from, to] window; {@code casesAssigned} and {@code activitiesCreated} reflect current
 * standing state rather than window activity, matching how caseload size is reported
 * elsewhere in this API. Kidaura's "IEP Created" has no {@code createdBy} field to key off in
 * this schema — {@code iepPlans} is the closest honest equivalent: plans whose therapist of
 * record is this member, created within the window.
 */
public record MemberSummaryResponse(
        UUID therapistId,
        String therapistName,
        String role,
        int casesAssigned,
        int activitiesCreated,
        int activitiesAssigned,
        int sessionsCancelled,
        int iepPlans
) {}

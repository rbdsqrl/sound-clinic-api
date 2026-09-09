package com.simplehearing.analytics.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The org-wide "Overview" tab — engagement/activity figures, distinct from
 * {@link OrgSnapshotResponse}'s clinical-outcome rollup and {@link TimeSeriesResponse}'s
 * goal-mastery trend. All figures are for the requested [from, to] window.
 */
public record EngagementOverviewResponse(
        UserCounts activeUsers,
        UserCounts invitedUsers,
        Integer avgSessionDurationMinutes,
        List<NameCount> skillsBreakdown,
        List<NameCount> ageGroups,
        List<SessionsTrendPoint> sessionsTrend,
        int totalSessions,
        List<TrendPoint> checklistFilledTrend,
        List<NameCount> mostAssignedActivities
) {
    /** Members = staff (therapist/clinic head/business owner); Cases = patients. */
    public record UserCounts(int members, int cases) {}

    public record NameCount(String name, int count) {}

    public record TrendPoint(LocalDate date, int count) {}

    /** byStatus keys are TherapySessionStatus names (COMPLETED, CANCELLED, ...) — a status with
     *  no sessions that day is simply absent from the map, not zero-filled. */
    public record SessionsTrendPoint(LocalDate date, Map<String, Integer> byStatus) {}
}

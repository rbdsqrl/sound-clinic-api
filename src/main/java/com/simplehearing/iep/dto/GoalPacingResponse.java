package com.simplehearing.iep.dto;

import com.simplehearing.iep.enums.IEPGoalStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * How a plan's goals are spread across the sessions of the therapy it's linked to.
 *
 * <p>The sessions still to come ({@code remainingSessions}) are divided between the plan's not-yet-completed
 * goals in order, so each gets a session budget and a suggested target date (the date of the last session in
 * its share). Because it works from the sessions that actually exist, it re-spreads by itself whenever the
 * therapy's frequency or dates change — "Apply" then writes the suggested dates onto the goals.
 */
public record GoalPacingResponse(
        /** False when the plan isn't linked to a therapy — nothing to pace against. */
        boolean linked,
        UUID planId,
        UUID enrollmentId,
        String programName,
        /** Sessions per week the therapy runs (from its session days), 0 when unknown. */
        double sessionsPerWeek,
        /** Sessions that count toward the therapy, excluding cancelled ones. */
        int totalSessions,
        int completedSessions,
        /** Upcoming sessions the active goals are spread across. */
        int remainingSessions,
        int activeGoals,
        /** True when the remaining sessions give the average goal fewer than {@value #MIN_SESSIONS_PER_GOAL} sessions. */
        boolean overloaded,
        /** A plain-language read of the situation, e.g. "9 sessions left for 4 goals — about 2 each." */
        String summary,
        List<GoalPace> goals
) {
    public static final int MIN_SESSIONS_PER_GOAL = 3;

    public record GoalPace(
            UUID goalId,
            String title,
            IEPGoalStatus status,
            /** Sessions allotted to this goal; 0 when there are more goals than sessions left. Null for completed goals. */
            Integer sessionsBudget,
            LocalDate suggestedTargetDate,
            LocalDate currentTargetDate,
            /** The goal's current target date has already passed and it isn't completed. */
            boolean overdue,
            /** The current target date is missing or more than a week from the suggested one. */
            boolean differsFromSuggested
    ) {}
}

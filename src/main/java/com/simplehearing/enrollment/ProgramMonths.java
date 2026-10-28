package com.simplehearing.enrollment;

import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.TreeMap;

/**
 * Splits a program's sessions by calendar month, so every screen (parent, therapist, admin) quotes the same
 * "this month's" count instead of one running total for the whole plan. A session counts toward its month when
 * it is part of the paid plan (not an extra) and hasn't been cancelled. The website's {@code lib/sessionMonths.ts}
 * applies the same rule to the session list — keep the two in step.
 */
public final class ProgramMonths {

    private ProgramMonths() {}

    /** One month's slice: how many plan sessions fall in it and how many of those are completed. */
    public record Summary(YearMonth month, int completed, int total) {}

    private record Tally(int completed, int total) {}

    /** The month a program is "in" today: this month if it has sessions, else the next month that does, else the last one. */
    public static Summary current(List<TherapySession> sessions, LocalDate today) {
        TreeMap<YearMonth, Tally> byMonth = new TreeMap<>();
        for (TherapySession s : sessions) {
            if (!s.isCountsTowardPlan() || s.getStatus() == TherapySessionStatus.CANCELLED) continue;
            byMonth.merge(YearMonth.from(s.getSessionDate()),
                    new Tally(s.getStatus() == TherapySessionStatus.COMPLETED ? 1 : 0, 1),
                    (a, b) -> new Tally(a.completed() + b.completed(), a.total() + b.total()));
        }
        YearMonth now = YearMonth.from(today);
        if (byMonth.isEmpty()) return new Summary(now, 0, 0);

        YearMonth pick = byMonth.containsKey(now) ? now
                : byMonth.ceilingKey(now) != null ? byMonth.ceilingKey(now)
                : byMonth.lastKey();
        Tally t = byMonth.get(pick);
        return new Summary(pick, t.completed(), t.total());
    }
}

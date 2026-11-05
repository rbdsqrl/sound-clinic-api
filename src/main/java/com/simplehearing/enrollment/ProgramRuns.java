package com.simplehearing.enrollment;

import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Splits a program's regular sessions into its runs — the original run and each renewal. The original sessions and
 * every renewal's added sessions are created as separate batches, so sessions created within a minute of each other
 * belong to the same run. Extra (ad hoc) sessions are not part of any run. The migration that fills in older payments'
 * periods (136) applies the same rule in SQL — keep the two in step.
 */
public final class ProgramRuns {

    private ProgramRuns() {}

    /** One run: its 1-based number (1 = the original run), first and last session date, and how many sessions it holds. */
    public record Run(int number, LocalDate startDate, LocalDate endDate, int sessionCount) {}

    private static final Duration SAME_BATCH = Duration.ofMinutes(1);

    public static List<Run> of(List<TherapySession> sessions) {
        List<TherapySession> regular = sessions.stream()
                .filter(s -> !s.isAdHoc() && s.getCreatedAt() != null)
                .sorted(Comparator.comparing(TherapySession::getCreatedAt))
                .toList();

        List<List<TherapySession>> batches = new ArrayList<>();
        TherapySession previous = null;
        for (TherapySession s : regular) {
            if (previous == null || Duration.between(previous.getCreatedAt(), s.getCreatedAt()).compareTo(SAME_BATCH) > 0) {
                batches.add(new ArrayList<>());
            }
            batches.get(batches.size() - 1).add(s);
            previous = s;
        }

        List<Run> runs = new ArrayList<>();
        for (List<TherapySession> batch : batches) {
            // A run whose sessions were all cancelled still has the dates it was planned for.
            List<TherapySession> live = batch.stream().filter(s -> s.getStatus() != TherapySessionStatus.CANCELLED).toList();
            List<TherapySession> dated = live.isEmpty() ? batch : live;
            LocalDate start = dated.stream().map(TherapySession::getSessionDate).min(LocalDate::compareTo).orElse(null);
            LocalDate end = dated.stream().map(TherapySession::getSessionDate).max(LocalDate::compareTo).orElse(null);
            runs.add(new Run(runs.size() + 1, start, end, live.size()));
        }
        return runs;
    }
}

package com.simplehearing.analytics.dto;

import com.simplehearing.analytics.enums.Granularity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The single envelope every analytics chart consumes, whatever the subject is.
 *
 * <p>Buckets are always contiguous and zero-filled across the requested window: a period
 * with no recorded trials still appears, with {@code masteryPct} null. Null means "nothing
 * was logged", never "scored zero" — the frontend draws it as a break in the line rather
 * than a drop to the floor.
 */
public record TimeSeriesResponse(
        SubjectType subjectType,
        UUID        subjectId,
        String      subjectName,
        Granularity granularity,
        LocalDate   from,
        LocalDate   to,
        List<Bucket>       buckets,
        List<DomainSeries> domains,
        Totals             totals
) {

    public enum SubjectType { PATIENT, THERAPIST, ORGANISATION }

    /**
     * One period on the x-axis.
     *
     * @param masteryPct  Sigma(trialsPassed) / Sigma(trialsTotal) for the period, 0-100, one decimal.
     *                    Null when no trials were logged. Deliberately a ratio of sums rather than
     *                    an average of per-session ratios, so a 1-of-1 session cannot outweigh an 18-of-20 one.
     */
    public record Bucket(
            LocalDate periodStart,
            String    label,
            Double    masteryPct,
            int       trialsPassed,
            int       trialsTotal,
            int       sessionsCompleted,
            int       sessionsNoShow,
            int       sessionsCancelled,
            int       sessionsRescheduled,
            int       sessionsLogged,
            Double    avgPerformanceScore,
            Double    avgParentRating
    ) {}

    /**
     * A per-domain mastery series aligned index-for-index with {@link #buckets}, so the frontend
     * can draw small multiples without a second request.
     *
     * @param plateau true when the domain moved less than {@link DomainSeries#PLATEAU_THRESHOLD_PTS}
     *                points across the window despite having enough data to judge.
     */
    public record DomainSeries(
            String       domain,
            List<Double> masteryPct,
            Double       current,
            Double       deltaPts,
            int          trialsTotal,
            boolean      plateau
    ) {
        /** Below this much movement across the window, a domain is flagged as stalled. */
        public static final double PLATEAU_THRESHOLD_PTS = 8.0;

        /** A domain needs at least this many populated buckets before a plateau call is meaningful. */
        public static final int PLATEAU_MIN_BUCKETS = 4;
    }

    /**
     * Window-level rollup.
     *
     * @param coveragePct share of completed sessions that carry any therapist input at all
     *                    (notes, progress report, or a score). A trend built on thin coverage
     *                    is a sampling artefact, so this always travels with the series.
     */
    public record Totals(
            Double masteryPct,
            Double masteryDeltaPts,
            int    trialsPassed,
            int    trialsTotal,
            int    sessionsScheduled,
            int    sessionsCompleted,
            int    sessionsNoShow,
            int    sessionsCancelled,
            int    sessionsLogged,
            Double coveragePct,
            int    goalsTotal,
            int    goalsCompleted,
            Double avgPerformanceScore,
            Double avgParentRating
    ) {}
}

package com.simplehearing.session.repository;

import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TherapySessionRepository extends JpaRepository<TherapySession, UUID> {

    /** All sessions for an org in a date range (admin/owner calendar view) */
    List<TherapySession> findByOrgIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            UUID orgId, LocalDate from, LocalDate to);

    /**
     * Lightweight session rows for a date range — one query that joins patient, therapist and
     * program names in, instead of loading full session rows and then looking each name up
     * separately. The two filters are switchable (a flag plus a dummy value when off) rather than
     * nullable, since Postgres can't infer the type of a null parameter in a comparison.
     */
    @Query("""
            SELECT new com.simplehearing.session.dto.TherapySessionSummaryResponse(
                   s.id, s.enrollmentId, s.patientId, p.firstName, p.lastName,
                   s.therapistId, t.firstName, t.lastName,
                   COALESCE(pr.name, 'Unknown Program'),
                   s.sessionDate, s.startTime, s.endTime, s.status)
            FROM TherapySession s
            LEFT JOIN Patient p ON p.id = s.patientId
            LEFT JOIN User t ON t.id = s.therapistId
            LEFT JOIN Enrollment e ON e.id = s.enrollmentId
            LEFT JOIN Subscription sub ON sub.id = e.subscriptionId
            LEFT JOIN Program pr ON pr.id = sub.programId
            WHERE s.orgId = :orgId AND s.sessionDate BETWEEN :from AND :to
              AND (:byTherapist = false OR s.therapistId = :therapistId)
              AND (:byPatients = false OR s.patientId IN :patientIds)
            ORDER BY s.sessionDate ASC, s.startTime ASC
            """)
    List<com.simplehearing.session.dto.TherapySessionSummaryResponse> findSummaries(
            @Param("orgId") UUID orgId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("byTherapist") boolean byTherapist,
            @Param("therapistId") UUID therapistId,
            @Param("byPatients") boolean byPatients,
            @Param("patientIds") Collection<UUID> patientIds);

    /** Per-patient count of SCHEDULED/PENDING_RESCHEDULE sessions in a date range — powers the
     *  Cases analytics tab's "Upcoming" column without pulling every matching session's full
     *  row into memory just to count it (that window can span up to MAX_WINDOW_DAYS forward). */
    @Query("SELECT s.patientId, COUNT(s) FROM TherapySession s "
         + "WHERE s.orgId = :orgId AND s.sessionDate BETWEEN :from AND :to "
         + "AND s.status IN (com.simplehearing.session.enums.TherapySessionStatus.SCHEDULED, "
         +                  "com.simplehearing.session.enums.TherapySessionStatus.PENDING_RESCHEDULE) "
         + "GROUP BY s.patientId")
    List<Object[]> countUpcomingByPatient(@Param("orgId") UUID orgId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    /** Per-therapist count of CANCELLED / CANCELLATION_REQUESTED sessions in a date range — the
     *  Members analytics tab's "Sessions Cancelled" column, without loading the window's sessions. */
    @Query("SELECT s.therapistId, COUNT(s) FROM TherapySession s "
         + "WHERE s.orgId = :orgId AND s.sessionDate BETWEEN :from AND :to "
         + "AND s.status IN (com.simplehearing.session.enums.TherapySessionStatus.CANCELLED, "
         +                  "com.simplehearing.session.enums.TherapySessionStatus.CANCELLATION_REQUESTED) "
         + "GROUP BY s.therapistId")
    List<Object[]> countCancelledByTherapist(@Param("orgId") UUID orgId,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    /** Sessions still flagged PENDING_RESCHEDULE whose date has already gone by unaddressed —
     *  the daily auto-cancel sweep (see session.job.MissedRescheduleCancelJob). */
    List<TherapySession> findByStatusAndSessionDateBefore(TherapySessionStatus status, LocalDate date);

    /** Therapist's own sessions in a date range */
    List<TherapySession> findByOrgIdAndTherapistIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            UUID orgId, UUID therapistId, LocalDate from, LocalDate to);

    /** Patient's sessions in a date range */
    List<TherapySession> findByOrgIdAndPatientIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            UUID orgId, UUID patientId, LocalDate from, LocalDate to);

    /** A parent's children's sessions in a date range (calendar view) */
    List<TherapySession> findByOrgIdAndPatientIdInAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            UUID orgId, List<UUID> patientIds, LocalDate from, LocalDate to);

    /** All sessions belonging to a specific enrollment (for detail view) */
    List<TherapySession> findByEnrollmentIdOrderBySessionNumberAsc(UUID enrollmentId);

    /** Batched form of the above — used by EnrollmentPaymentActivationService to flip
     *  awaitingPayment across every session of every enrollment a subscription covers, in one
     *  round trip. */
    List<TherapySession> findByEnrollmentIdIn(Collection<UUID> enrollmentIds);

    /** How many sessions of a plan the parent has already asked to move. */
    int countByEnrollmentIdAndParentRescheduleRequestedTrue(UUID enrollmentId);

    /** Batched form of the above — one round trip for every enrollment touched by a session list, instead of one per enrollment. */
    @Query("SELECT s.enrollmentId, COUNT(s) FROM TherapySession s " +
           "WHERE s.enrollmentId IN :enrollmentIds AND s.parentRescheduleRequested = true " +
           "GROUP BY s.enrollmentId")
    List<Object[]> countParentReschedulesByEnrollmentIds(@Param("enrollmentIds") java.util.Collection<UUID> enrollmentIds);

    /** Live count of completed sessions that count toward the plan — the source of truth for an
     *  enrollment's progress. Deliberately not a stored counter on Enrollment: a counter needs every
     *  write path (including seed/import data) to remember to keep it in sync, and one didn't. */
    int countByEnrollmentIdAndStatusAndCountsTowardPlanTrue(UUID enrollmentId, TherapySessionStatus status);

    /** All sessions in a specific status for an org (e.g. PENDING_RESCHEDULE) */
    @Query("SELECT s FROM TherapySession s WHERE s.orgId = :orgId AND s.status = :status " +
           "ORDER BY s.sessionDate ASC, s.startTime ASC")
    List<TherapySession> findByOrgIdAndStatus(
            @Param("orgId") UUID orgId, @Param("status") TherapySessionStatus status);

    long countByOrgIdAndStatus(UUID orgId, TherapySessionStatus status);

    /** All PENDING_RESCHEDULE sessions for the dashboard (covers leave, holiday, and parent requests) */
    @Query("SELECT s FROM TherapySession s WHERE s.orgId = :orgId " +
           "AND s.status = com.simplehearing.session.enums.TherapySessionStatus.PENDING_RESCHEDULE " +
           "ORDER BY s.sessionDate ASC, s.startTime ASC")
    List<TherapySession> findAllPendingReschedule(@Param("orgId") UUID orgId);

    /** Sessions on a specific date in a given status (used when creating public holidays) */
    @Query("SELECT s FROM TherapySession s WHERE s.orgId = :orgId " +
           "AND s.sessionDate = :sessionDate AND s.status = :status")
    List<TherapySession> findByOrgIdAndSessionDateAndStatus(
            @Param("orgId") UUID orgId,
            @Param("sessionDate") java.time.LocalDate sessionDate,
            @Param("status") TherapySessionStatus status);

    /** Sessions for a specific therapist on a specific date in a given status */
    @Query("SELECT s FROM TherapySession s WHERE s.orgId = :orgId AND s.therapistId = :therapistId " +
           "AND s.sessionDate = :sessionDate AND s.status = :status")
    List<TherapySession> findByOrgIdAndTherapistIdAndSessionDateAndStatus(
            @Param("orgId") UUID orgId, @Param("therapistId") UUID therapistId,
            @Param("sessionDate") LocalDate sessionDate, @Param("status") TherapySessionStatus status);

    /** Sessions for a specific therapist across a date range (inclusive) in a given status —
     *  used to flag sessions affected by a leave request that spans multiple days. */
    @Query("SELECT s FROM TherapySession s WHERE s.orgId = :orgId AND s.therapistId = :therapistId " +
           "AND s.sessionDate BETWEEN :startDate AND :endDate AND s.status = :status")
    List<TherapySession> findByOrgIdAndTherapistIdAndSessionDateBetweenAndStatus(
            @Param("orgId") UUID orgId, @Param("therapistId") UUID therapistId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("status") TherapySessionStatus status);

    List<TherapySession> findByPatientId(UUID patientId);

    /** Sessions currently owned by a specific bulk therapist reassignment — the revert scan. */
    List<TherapySession> findByReassignmentId(UUID reassignmentId);

    /** Readable alias used by the analytics module — same query as the derived name above. */
    @Query("SELECT s FROM TherapySession s WHERE s.orgId = :orgId AND s.patientId = :patientId "
         + "AND s.sessionDate BETWEEN :from AND :to ORDER BY s.sessionDate ASC, s.startTime ASC")
    List<TherapySession> findByOrgIdAndPatientIdBetween(
            @Param("orgId") UUID orgId, @Param("patientId") UUID patientId,
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Readable alias used by the analytics module — same query as the derived name above. */
    @Query("SELECT s FROM TherapySession s WHERE s.orgId = :orgId AND s.therapistId = :therapistId "
         + "AND s.sessionDate BETWEEN :from AND :to ORDER BY s.sessionDate ASC, s.startTime ASC")
    List<TherapySession> findByOrgIdAndTherapistIdBetween(
            @Param("orgId") UUID orgId, @Param("therapistId") UUID therapistId,
            @Param("from") LocalDate from, @Param("to") LocalDate to);


    /** "Checklist filled" predicate shared by the analytics counts below: the session's checklist
     *  note is non-blank, or it has a feedback answer with text or at least one selected option.
     *  A feedback-answer row alone doesn't count — the frontend writes one per template question
     *  on every save regardless of whether anything was ticked. */
    String CHECKLIST_FILLED =
            "((s.checklistNotes IS NOT NULL AND TRIM(s.checklistNotes) <> '') "
          + "OR EXISTS (SELECT 1 FROM SessionFeedbackAnswer a WHERE a.sessionId = s.id "
          +            "AND ((a.textAnswer IS NOT NULL AND TRIM(a.textAnswer) <> '') "
          +                 "OR EXISTS (SELECT 1 FROM SessionFeedbackAnswerOption o WHERE o.id.answerId = a.id))))";

    /** Per-patient, per-status session counts in a date range — the Cases tab's attended/cancelled
     *  columns, without loading the window's sessions just to tally their status. */
    @Query("SELECT s.patientId, s.status, COUNT(s) FROM TherapySession s "
         + "WHERE s.orgId = :orgId AND s.sessionDate BETWEEN :from AND :to "
         + "GROUP BY s.patientId, s.status")
    List<Object[]> countByPatientAndStatus(@Param("orgId") UUID orgId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    /** Per-patient count of sessions in the window whose feedback checklist was engaged with. */
    @Query("SELECT s.patientId, COUNT(s) FROM TherapySession s "
         + "WHERE s.orgId = :orgId AND s.sessionDate BETWEEN :from AND :to AND " + CHECKLIST_FILLED + " "
         + "GROUP BY s.patientId")
    List<Object[]> countChecklistFilledByPatient(@Param("orgId") UUID orgId,
                                                 @Param("from") LocalDate from,
                                                 @Param("to") LocalDate to);

    /** Per-day count of sessions in the window whose feedback checklist was engaged with. */
    @Query("SELECT s.sessionDate, COUNT(s) FROM TherapySession s "
         + "WHERE s.orgId = :orgId AND s.sessionDate BETWEEN :from AND :to AND " + CHECKLIST_FILLED + " "
         + "GROUP BY s.sessionDate")
    List<Object[]> countChecklistFilledByDate(@Param("orgId") UUID orgId,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);

    /** Sessions per calendar day — feeds the activity heatmap without loading the sessions. */
    @Query("SELECT s.sessionDate, COUNT(s) FROM TherapySession s "
         + "WHERE s.orgId = :orgId AND s.sessionDate BETWEEN :from AND :to "
         + "GROUP BY s.sessionDate ORDER BY s.sessionDate")
    List<Object[]> countByDate(@Param("orgId") UUID orgId,
                               @Param("from") LocalDate from,
                               @Param("to") LocalDate to);

    /** Sessions per calendar day per status — the Overview tab's sessions trend. */
    @Query("SELECT s.sessionDate, s.status, COUNT(s) FROM TherapySession s "
         + "WHERE s.orgId = :orgId AND s.sessionDate BETWEEN :from AND :to "
         + "GROUP BY s.sessionDate, s.status ORDER BY s.sessionDate")
    List<Object[]> countByDateAndStatus(@Param("orgId") UUID orgId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    /** Start/end times of COMPLETED sessions in the window — all the average-duration figure
     *  needs, instead of every column (including four TEXT ones) of every session. */
    @Query("SELECT s.startTime, s.endTime FROM TherapySession s "
         + "WHERE s.orgId = :orgId AND s.sessionDate BETWEEN :from AND :to "
         + "AND s.status = com.simplehearing.session.enums.TherapySessionStatus.COMPLETED")
    List<Object[]> findCompletedTimes(@Param("orgId") UUID orgId,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    /** Sessions in a date range that belong to any of the given enrollments (Schedule tab's program filter). */
    @Query("SELECT s FROM TherapySession s WHERE s.orgId = :orgId AND s.enrollmentId IN :enrollmentIds "
         + "AND s.sessionDate BETWEEN :from AND :to ORDER BY s.sessionDate ASC, s.startTime ASC")
    List<TherapySession> findByOrgIdAndEnrollmentIdInBetween(
            @Param("orgId") UUID orgId, @Param("enrollmentIds") Collection<UUID> enrollmentIds,
            @Param("from") LocalDate from, @Param("to") LocalDate to);
}

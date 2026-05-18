package com.simplehearing.session.repository;

import com.simplehearing.session.entity.TherapySession;
import com.simplehearing.session.enums.TherapySessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TherapySessionRepository extends JpaRepository<TherapySession, UUID> {

    /** All sessions for an org in a date range (admin/owner calendar view) */
    List<TherapySession> findByOrgIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            UUID orgId, LocalDate from, LocalDate to);

    /** Therapist's own sessions in a date range */
    List<TherapySession> findByOrgIdAndTherapistIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            UUID orgId, UUID therapistId, LocalDate from, LocalDate to);

    /** Patient's sessions in a date range */
    List<TherapySession> findByOrgIdAndPatientIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            UUID orgId, UUID patientId, LocalDate from, LocalDate to);

    /** All sessions belonging to a specific enrollment (for detail view) */
    List<TherapySession> findByEnrollmentIdOrderBySessionNumberAsc(UUID enrollmentId);

    /** All sessions in a specific status for an org (e.g. PENDING_RESCHEDULE) */
    @Query("SELECT s FROM TherapySession s WHERE s.orgId = :orgId AND s.status = :status " +
           "ORDER BY s.sessionDate ASC, s.startTime ASC")
    List<TherapySession> findByOrgIdAndStatus(
            @Param("orgId") UUID orgId, @Param("status") TherapySessionStatus status);

    /**
     * PENDING_RESCHEDULE sessions that have a corresponding APPROVED leave —
     * used by the dashboard so stale/incorrect status rows are never shown.
     */
    @Query("SELECT s FROM TherapySession s WHERE s.orgId = :orgId " +
           "AND s.status = com.simplehearing.session.enums.TherapySessionStatus.PENDING_RESCHEDULE " +
           "AND EXISTS (SELECT l FROM com.simplehearing.leave.entity.Leave l " +
           "            WHERE l.orgId = :orgId AND l.therapistId = s.therapistId " +
           "            AND l.leaveDate = s.sessionDate " +
           "            AND l.status = com.simplehearing.leave.enums.LeaveStatus.APPROVED) " +
           "ORDER BY s.sessionDate ASC, s.startTime ASC")
    List<TherapySession> findPendingRescheduleWithApprovedLeave(@Param("orgId") UUID orgId);

    /** Sessions for a specific therapist on a specific date in a given status */
    @Query("SELECT s FROM TherapySession s WHERE s.orgId = :orgId AND s.therapistId = :therapistId " +
           "AND s.sessionDate = :sessionDate AND s.status = :status")
    List<TherapySession> findByOrgIdAndTherapistIdAndSessionDateAndStatus(
            @Param("orgId") UUID orgId, @Param("therapistId") UUID therapistId,
            @Param("sessionDate") LocalDate sessionDate, @Param("status") TherapySessionStatus status);
}

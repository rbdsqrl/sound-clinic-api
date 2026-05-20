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
}

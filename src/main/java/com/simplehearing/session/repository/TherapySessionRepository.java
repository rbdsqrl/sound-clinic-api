package com.simplehearing.session.repository;

import com.simplehearing.session.entity.TherapySession;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

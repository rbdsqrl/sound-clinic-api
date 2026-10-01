package com.simplehearing.enrollment.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import com.simplehearing.enrollment.entity.Enrollment;
import com.simplehearing.enrollment.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    /** All enrollments for a patient within the org, newest first */
    List<Enrollment> findByOrgIdAndPatientIdOrderByCreatedAtDesc(UUID orgId, UUID patientId);

    /** A patient's enrollments currently under a given therapist, in a given status — the
     *  reassignment cascade's scope for one case. */
    List<Enrollment> findByOrgIdAndPatientIdAndTherapistIdAndStatus(
            UUID orgId, UUID patientId, UUID therapistId, EnrollmentStatus status);

    /** All active enrollments for a therapist within the org */
    List<Enrollment> findByOrgIdAndTherapistId(UUID orgId, UUID therapistId);

    /** Every enrollment in the org — used for org-wide rollups (duration, program breakdown). */
    List<Enrollment> findByOrgId(UUID orgId);

    /** The patient's current, still-open discharge episode — every enrollment not yet claimed by a past discharge. */
    List<Enrollment> findByOrgIdAndPatientIdAndDischargedInRecordIdIsNull(UUID orgId, UUID patientId);

    /** Every enrollment closed by a specific discharge episode — for building/re-reading its report. */
    List<Enrollment> findByDischargedInRecordId(UUID dischargeRecordId);

    /** Every enrollment tied to a subscription — normally one, but not assumed. Used by
     *  EnrollmentPaymentActivationService to flip awaitingPayment once payment clears. */
    List<Enrollment> findBySubscriptionId(UUID subscriptionId);

    void deleteByPatientId(UUID patientId);

    /** (startDate, endDate) of enrollments that have ended — all the average-duration figure needs. */
    @Query("SELECT e.startDate, e.endDate FROM Enrollment e WHERE e.orgId = :orgId AND e.endDate IS NOT NULL")
    List<Object[]> findEndedSpans(@Param("orgId") UUID orgId);

    /** (programName, distinct patients, enrollments) across the org, resolved through subscription -> program. */
    @Query("SELECT p.name, COUNT(DISTINCT e.patientId), COUNT(e) FROM Enrollment e, Subscription s, Program p "
         + "WHERE e.orgId = :orgId AND s.id = e.subscriptionId AND p.id = s.programId GROUP BY p.name")
    List<Object[]> countByProgramName(@Param("orgId") UUID orgId);

    /** (distinct patients, enrollments) whose subscription or program can't be resolved. */
    @Query("SELECT COUNT(DISTINCT e.patientId), COUNT(e) FROM Enrollment e WHERE e.orgId = :orgId "
         + "AND NOT EXISTS (SELECT 1 FROM Subscription s, Program p WHERE s.id = e.subscriptionId AND p.id = s.programId)")
    List<Object[]> countWithoutProgram(@Param("orgId") UUID orgId);

    /** Ids of enrollments running a given program (via their subscription). */
    @Query("SELECT e.id FROM Enrollment e, Subscription s WHERE e.orgId = :orgId AND s.id = e.subscriptionId AND s.programId = :programId")
    List<UUID> findIdsByProgram(@Param("orgId") UUID orgId, @Param("programId") UUID programId);
}

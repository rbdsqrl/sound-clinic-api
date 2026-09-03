package com.simplehearing.enrollment.repository;

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

    void deleteByPatientId(UUID patientId);
}

package com.simplehearing.enrollment.repository;

import com.simplehearing.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    /** All enrollments for a patient within the org, newest first */
    List<Enrollment> findByOrgIdAndPatientIdOrderByCreatedAtDesc(UUID orgId, UUID patientId);

    /** All active enrollments for a therapist within the org */
    List<Enrollment> findByOrgIdAndTherapistId(UUID orgId, UUID therapistId);
}

package com.simplehearing.concern.repository;

import com.simplehearing.concern.entity.EnrollmentConcern;
import com.simplehearing.concern.enums.ConcernStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConcernRepository extends JpaRepository<EnrollmentConcern, UUID> {

    List<EnrollmentConcern> findByOrgIdAndEnrollmentIdOrderByRaisedAtDesc(UUID orgId, UUID enrollmentId);

    List<EnrollmentConcern> findByOrgIdAndPatientIdOrderByRaisedAtDesc(UUID orgId, UUID patientId);

    List<EnrollmentConcern> findByOrgIdAndStatusOrderByRaisedAtDesc(UUID orgId, ConcernStatus status);

    List<EnrollmentConcern> findByOrgIdAndTherapistIdAndStatusOrderByRaisedAtDesc(
            UUID orgId, UUID therapistId, ConcernStatus status);

    List<EnrollmentConcern> findByOrgIdAndTherapistIdOrderByRaisedAtDesc(UUID orgId, UUID therapistId);

    List<EnrollmentConcern> findByOrgIdOrderByRaisedAtDesc(UUID orgId);

    int countByOrgIdAndStatus(UUID orgId, ConcernStatus status);

    int countByOrgIdAndTherapistIdAndStatus(UUID orgId, UUID therapistId, ConcernStatus status);
}

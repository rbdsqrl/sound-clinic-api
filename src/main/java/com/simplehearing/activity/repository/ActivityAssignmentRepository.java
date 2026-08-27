package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityAssignmentRepository extends JpaRepository<ActivityAssignment, UUID> {
    List<ActivityAssignment> findByOrgIdAndPatientIdOrderByCreatedAtDesc(UUID orgId, UUID patientId);
    Optional<ActivityAssignment> findByIdAndOrgId(UUID id, UUID orgId);
    long countByOrgIdAndPatientId(UUID orgId, UUID patientId);
    List<ActivityAssignment> findByOrgId(UUID orgId);
}

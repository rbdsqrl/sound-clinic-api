package com.simplehearing.resource.repository;

import com.simplehearing.resource.entity.ResourceAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceAssignmentRepository extends JpaRepository<ResourceAssignment, UUID> {

    List<ResourceAssignment> findByOrgIdAndPatientIdOrderByCreatedAtDesc(UUID orgId, UUID patientId);

    Optional<ResourceAssignment> findByResourceIdAndPatientId(UUID resourceId, UUID patientId);

    long countByResourceId(UUID resourceId);
}

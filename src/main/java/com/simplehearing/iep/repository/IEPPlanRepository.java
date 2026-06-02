package com.simplehearing.iep.repository;

import com.simplehearing.iep.entity.IEPPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IEPPlanRepository extends JpaRepository<IEPPlan, UUID> {

    List<IEPPlan> findByOrgIdAndPatientIdOrderByCreatedAtDesc(UUID orgId, UUID patientId);

    Optional<IEPPlan> findByIdAndOrgId(UUID id, UUID orgId);
}

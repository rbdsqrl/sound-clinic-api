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

    List<IEPPlan> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    Optional<IEPPlan> findByIdAndOrgId(UUID id, UUID orgId);

    List<IEPPlan> findByPatientId(UUID patientId);

    List<IEPPlan> findByOrgIdAndTherapistId(UUID orgId, UUID therapistId);

    void deleteByPatientId(UUID patientId);
}

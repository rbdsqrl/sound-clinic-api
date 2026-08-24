package com.simplehearing.discharge.repository;

import com.simplehearing.discharge.entity.DischargeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DischargeRecordRepository extends JpaRepository<DischargeRecord, UUID> {

    List<DischargeRecord> findByOrgIdAndPatientIdOrderByDischargeDateDesc(UUID orgId, UUID patientId);

    Optional<DischargeRecord> findByIdAndOrgId(UUID id, UUID orgId);
}

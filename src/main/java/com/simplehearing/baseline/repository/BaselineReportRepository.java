package com.simplehearing.baseline.repository;

import com.simplehearing.baseline.entity.BaselineReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BaselineReportRepository extends JpaRepository<BaselineReport, UUID> {
    Optional<BaselineReport> findByPatientId(UUID patientId);
    Optional<BaselineReport> findByIdAndOrgId(UUID id, UUID orgId);
}

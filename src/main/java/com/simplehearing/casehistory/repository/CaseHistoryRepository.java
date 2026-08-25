package com.simplehearing.casehistory.repository;

import com.simplehearing.casehistory.entity.CaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CaseHistoryRepository extends JpaRepository<CaseHistory, UUID> {

    Optional<CaseHistory> findByOrgIdAndPatientId(UUID orgId, UUID patientId);
}

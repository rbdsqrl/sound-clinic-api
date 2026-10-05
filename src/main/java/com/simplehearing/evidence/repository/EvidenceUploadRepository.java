package com.simplehearing.evidence.repository;

import com.simplehearing.evidence.entity.EvidenceUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceUploadRepository extends JpaRepository<EvidenceUpload, UUID> {

    Optional<EvidenceUpload> findByIdAndOrgIdAndPatientId(UUID id, UUID orgId, UUID patientId);

    List<EvidenceUpload> findByExpiresAtBefore(Instant cutoff);
}

package com.simplehearing.patient.repository;

import com.simplehearing.patient.entity.PatientParent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PatientParentRepository extends JpaRepository<PatientParent, PatientParent.Id> {

    List<PatientParent> findById_PatientId(UUID patientId);

    /** Bulk variant — avoids one query per patient when building a page of Cases. */
    List<PatientParent> findById_PatientIdIn(Collection<UUID> patientIds);

    List<PatientParent> findById_ParentId(UUID parentId);

    void deleteById_PatientIdAndId_ParentId(UUID patientId, UUID parentId);

    void deleteById_PatientId(UUID patientId);
}

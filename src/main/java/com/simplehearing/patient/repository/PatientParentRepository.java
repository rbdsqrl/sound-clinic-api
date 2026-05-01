package com.simplehearing.patient.repository;

import com.simplehearing.patient.entity.PatientParent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientParentRepository extends JpaRepository<PatientParent, PatientParent.Id> {

    List<PatientParent> findById_PatientId(UUID patientId);

    List<PatientParent> findById_ParentId(UUID parentId);

    void deleteById_PatientIdAndId_ParentId(UUID patientId, UUID parentId);
}

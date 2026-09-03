package com.simplehearing.patient.repository;

import com.simplehearing.patient.entity.PatientCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PatientConditionRepository extends JpaRepository<PatientCondition, PatientCondition.Id> {

    List<PatientCondition> findById_PatientId(UUID patientId);

    /** Bulk variant — avoids one query per patient when building a page of Cases. */
    List<PatientCondition> findById_PatientIdIn(Collection<UUID> patientIds);

    void deleteById_PatientIdAndId_ConditionId(UUID patientId, UUID conditionId);

    void deleteById_PatientId(UUID patientId);
}

package com.simplehearing.patient.repository;

import com.simplehearing.patient.entity.PatientCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientConditionRepository extends JpaRepository<PatientCondition, PatientCondition.Id> {

    List<PatientCondition> findById_PatientId(UUID patientId);

    void deleteById_PatientIdAndId_ConditionId(UUID patientId, UUID conditionId);
}

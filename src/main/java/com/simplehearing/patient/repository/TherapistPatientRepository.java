package com.simplehearing.patient.repository;

import com.simplehearing.patient.entity.TherapistPatient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TherapistPatientRepository extends JpaRepository<TherapistPatient, UUID> {

    List<TherapistPatient> findByPatientIdAndIsActive(UUID patientId, boolean isActive);

    List<TherapistPatient> findByTherapistIdAndIsActive(UUID therapistId, boolean isActive);

    Optional<TherapistPatient> findByPatientIdAndTherapistId(UUID patientId, UUID therapistId);
}

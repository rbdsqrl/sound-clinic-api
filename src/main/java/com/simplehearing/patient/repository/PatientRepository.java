package com.simplehearing.patient.repository;

import com.simplehearing.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    List<Patient> findByOrgId(UUID orgId);

    List<Patient> findByClinicId(UUID clinicId);

    Optional<Patient> findByIdAndOrgId(UUID id, UUID orgId);
}

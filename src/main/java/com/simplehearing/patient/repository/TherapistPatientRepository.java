package com.simplehearing.patient.repository;

import com.simplehearing.patient.entity.TherapistPatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TherapistPatientRepository extends JpaRepository<TherapistPatient, UUID> {

    List<TherapistPatient> findByPatientIdAndIsActive(UUID patientId, boolean isActive);

    List<TherapistPatient> findByTherapistIdAndIsActive(UUID therapistId, boolean isActive);

    Optional<TherapistPatient> findByPatientIdAndTherapistId(UUID patientId, UUID therapistId);

    @Query("SELECT tp.therapistId, COUNT(tp) FROM TherapistPatient tp WHERE tp.therapistId IN :ids AND tp.isActive = true GROUP BY tp.therapistId")
    List<Object[]> countCasesByTherapistIds(@Param("ids") List<UUID> ids);
}

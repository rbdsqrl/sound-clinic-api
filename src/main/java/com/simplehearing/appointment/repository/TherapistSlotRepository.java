package com.simplehearing.appointment.repository;

import com.simplehearing.appointment.entity.TherapistSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TherapistSlotRepository extends JpaRepository<TherapistSlot, UUID> {

    List<TherapistSlot> findByOrgId(UUID orgId);

    List<TherapistSlot> findByTherapistIdAndOrgId(UUID therapistId, UUID orgId);

    Optional<TherapistSlot> findByIdAndOrgId(UUID id, UUID orgId);
}

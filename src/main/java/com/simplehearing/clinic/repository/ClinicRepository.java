package com.simplehearing.clinic.repository;

import com.simplehearing.clinic.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    List<Clinic> findByOrgId(UUID orgId);

    Optional<Clinic> findByIdAndOrgId(UUID id, UUID orgId);
}

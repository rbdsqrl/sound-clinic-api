package com.simplehearing.clinic.repository;

import com.simplehearing.clinic.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    Optional<Clinic> findBySubdomain(String subdomain);

    boolean existsBySubdomain(String subdomain);
}

package com.simplehearing.organisation.repository;

import com.simplehearing.organisation.entity.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {

    boolean existsBySlug(String slug);

    Optional<Organisation> findBySlug(String slug);
}

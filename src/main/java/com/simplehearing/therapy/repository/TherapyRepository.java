package com.simplehearing.therapy.repository;

import com.simplehearing.therapy.entity.Therapy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TherapyRepository extends JpaRepository<Therapy, UUID> {

    List<Therapy> findByOrgIdAndIsActiveTrueOrderByNameAsc(UUID orgId);
}

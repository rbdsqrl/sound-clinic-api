package com.simplehearing.program.repository;

import com.simplehearing.program.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    /** All programs for an org ordered alphabetically */
    List<Program> findByOrgIdOrderByNameAsc(UUID orgId);

    /** Only active programs for an org (used when selecting a program for a subscription) */
    List<Program> findByOrgIdAndIsActiveTrueOrderByNameAsc(UUID orgId);
}

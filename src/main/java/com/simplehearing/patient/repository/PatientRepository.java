package com.simplehearing.patient.repository;

import com.simplehearing.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    List<Patient> findByOrgId(UUID orgId);

    /** Cheap paginated fallback for requests with no search/status/caseload narrowing — a plain indexed org_id + created_at scan, no EXISTS subqueries. */
    Page<Patient> findByOrgId(UUID orgId, Pageable pageable);

    List<Patient> findByClinicId(UUID clinicId);

    Optional<Patient> findByIdAndOrgId(UUID id, UUID orgId);

    /**
     * Backs the paginated Cases list. A case is Active until discharged — Inactive means
     * stage = DISCHARGED, nothing else. (Previously this also split out an "invite" status
     * derived from whether a parent was linked; dropped since it didn't correspond to anything
     * the UI should treat as a distinct case state.)
     */
    @Query("""
            SELECT p FROM Patient p WHERE p.orgId = :orgId
              AND (:search = '' OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:onlyMine = false OR EXISTS (
                    SELECT 1 FROM TherapistPatient tp
                    WHERE tp.patientId = p.id AND tp.therapistId = :userId AND tp.isActive = true))
              AND (
                    (:includeActive = true AND p.stage <> com.simplehearing.patient.enums.PatientStage.DISCHARGED)
                 OR (:includeInactive = true AND p.stage = com.simplehearing.patient.enums.PatientStage.DISCHARGED)
                  )
            """)
    Page<Patient> search(@Param("orgId") UUID orgId,
                          @Param("search") String search,
                          @Param("onlyMine") boolean onlyMine,
                          @Param("userId") UUID userId,
                          @Param("includeActive") boolean includeActive,
                          @Param("includeInactive") boolean includeInactive,
                          Pageable pageable);
}

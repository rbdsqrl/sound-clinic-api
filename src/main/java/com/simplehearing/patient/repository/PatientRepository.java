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

    List<Patient> findByClinicId(UUID clinicId);

    Optional<Patient> findByIdAndOrgId(UUID id, UUID orgId);

    /**
     * Backs the paginated Cases list. The three include* flags cover the three fixed status
     * categories the UI filters by (a patient can only ever land in exactly one) — invite status
     * (ACTIVE/NOT_INVITED) is derived from whether any parent is linked, not a stored column.
     */
    @Query("""
            SELECT p FROM Patient p WHERE p.orgId = :orgId
              AND (:search = '' OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:onlyMine = false OR EXISTS (
                    SELECT 1 FROM TherapistPatient tp
                    WHERE tp.patientId = p.id AND tp.therapistId = :userId AND tp.isActive = true))
              AND (
                    (:includeActive = true AND p.isActive = true AND EXISTS (SELECT 1 FROM PatientParent pp WHERE pp.id.patientId = p.id))
                 OR (:includeNotInvited = true AND p.isActive = true AND NOT EXISTS (SELECT 1 FROM PatientParent pp WHERE pp.id.patientId = p.id))
                 OR (:includeInactive = true AND p.isActive = false)
                  )
            """)
    Page<Patient> search(@Param("orgId") UUID orgId,
                          @Param("search") String search,
                          @Param("onlyMine") boolean onlyMine,
                          @Param("userId") UUID userId,
                          @Param("includeActive") boolean includeActive,
                          @Param("includeNotInvited") boolean includeNotInvited,
                          @Param("includeInactive") boolean includeInactive,
                          Pageable pageable);
}

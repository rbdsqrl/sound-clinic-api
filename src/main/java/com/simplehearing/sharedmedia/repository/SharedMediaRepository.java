package com.simplehearing.sharedmedia.repository;

import com.simplehearing.sharedmedia.entity.SharedMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SharedMediaRepository extends JpaRepository<SharedMedia, UUID> {

    List<SharedMedia> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    /** Posts of one direction across several children — a recipient's dashboard inbox. */
    List<SharedMedia> findByPatientIdInAndDirectionAndReadAtIsNullOrderByCreatedAtDesc(
            Collection<UUID> patientIds, com.simplehearing.sharedmedia.enums.SharedMediaDirection direction, Pageable page);

    List<SharedMedia> findByPatientIdInAndDirectionAndReadAtIsNotNullOrderByCreatedAtDesc(
            Collection<UUID> patientIds, com.simplehearing.sharedmedia.enums.SharedMediaDirection direction, Pageable page);

    long countByPatientIdInAndDirectionAndReadAtIsNull(
            Collection<UUID> patientIds, com.simplehearing.sharedmedia.enums.SharedMediaDirection direction);

    /** One therapist's Media & Notes uploads on a given day — see TherapistActivityService. */
    List<SharedMedia> findByOrgIdAndUploadedByAndCreatedAtBetween(
            UUID orgId, UUID uploadedBy, Instant start, Instant end);
}

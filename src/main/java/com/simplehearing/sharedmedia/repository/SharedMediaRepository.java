package com.simplehearing.sharedmedia.repository;

import com.simplehearing.sharedmedia.entity.SharedMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SharedMediaRepository extends JpaRepository<SharedMedia, UUID> {

    List<SharedMedia> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}

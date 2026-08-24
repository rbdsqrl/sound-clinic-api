package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findByOrgIdAndIsActiveTrueOrderByCreatedAtDesc(UUID orgId);

    List<Activity> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    Optional<Activity> findByIdAndOrgId(UUID id, UUID orgId);

    List<Activity> findByIsSharedTrueAndIsActiveTrueOrderByCreatedAtDesc();
}

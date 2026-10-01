package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findByOrgIdAndIsActiveTrueOrderByCreatedAtDesc(UUID orgId);

    List<Activity> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    /** Per-author count of an org's activities — the Members analytics tab's "Activities Created"
     *  column, without loading every activity row just to tally createdBy. */
    @Query("SELECT a.createdBy, COUNT(a) FROM Activity a WHERE a.orgId = :orgId AND a.createdBy IS NOT NULL GROUP BY a.createdBy")
    List<Object[]> countCreatedByAuthor(@Param("orgId") UUID orgId);

    Optional<Activity> findByIdAndOrgId(UUID id, UUID orgId);

    List<Activity> findByIsSharedTrueAndIsActiveTrueOrderByCreatedAtDesc();
}

package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityResourceRepository extends JpaRepository<ActivityResource, UUID> {
    List<ActivityResource> findByActivityIdOrderByCreatedAtAsc(UUID activityId);
}

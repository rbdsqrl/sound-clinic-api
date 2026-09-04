package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityLinkedResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ActivityLinkedResourceRepository extends JpaRepository<ActivityLinkedResource, UUID> {
    List<ActivityLinkedResource> findByActivityIdOrderByOrderIndexAsc(UUID activityId);

    @Transactional
    void deleteByActivityId(UUID activityId);
}

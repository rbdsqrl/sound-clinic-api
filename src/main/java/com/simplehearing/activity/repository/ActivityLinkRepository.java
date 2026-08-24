package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ActivityLinkRepository extends JpaRepository<ActivityLink, UUID> {
    List<ActivityLink> findByActivityIdOrderByOrderIndexAsc(UUID activityId);

    @Transactional
    void deleteByActivityId(UUID activityId);
}

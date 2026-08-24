package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityProp;
import com.simplehearing.activity.entity.ActivityPropId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ActivityPropRepository extends JpaRepository<ActivityProp, ActivityPropId> {

    List<ActivityProp> findById_ActivityId(UUID activityId);

    @Query("SELECT p FROM ActivityProp p WHERE p.id.activityId IN :activityIds")
    List<ActivityProp> findByActivityIdIn(@Param("activityIds") List<UUID> activityIds);

    @Transactional
    void deleteById_ActivityId(UUID activityId);
}

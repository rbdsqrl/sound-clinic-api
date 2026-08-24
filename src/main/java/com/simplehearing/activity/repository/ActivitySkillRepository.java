package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivitySkill;
import com.simplehearing.activity.entity.ActivitySkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ActivitySkillRepository extends JpaRepository<ActivitySkill, ActivitySkillId> {

    List<ActivitySkill> findById_ActivityId(UUID activityId);

    @Query("SELECT s FROM ActivitySkill s WHERE s.id.activityId IN :activityIds")
    List<ActivitySkill> findByActivityIdIn(@Param("activityIds") List<UUID> activityIds);

    @Transactional
    void deleteById_ActivityId(UUID activityId);
}

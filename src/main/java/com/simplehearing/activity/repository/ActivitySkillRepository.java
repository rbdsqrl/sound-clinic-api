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

    /** (skillId, count) over every activity in the org — the Overview tab's skills breakdown. */
    @Query("SELECT sk.id.skillId, COUNT(sk) FROM ActivitySkill sk, Activity a WHERE sk.id.activityId = a.id AND a.orgId = :orgId GROUP BY sk.id.skillId")
    List<Object[]> countBySkill(@Param("orgId") UUID orgId);
}

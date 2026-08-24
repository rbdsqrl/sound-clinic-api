package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityLanguage;
import com.simplehearing.activity.entity.ActivityLanguageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ActivityLanguageRepository extends JpaRepository<ActivityLanguage, ActivityLanguageId> {

    List<ActivityLanguage> findById_ActivityId(UUID activityId);

    @Query("SELECT l FROM ActivityLanguage l WHERE l.id.activityId IN :activityIds")
    List<ActivityLanguage> findByActivityIdIn(@Param("activityIds") List<UUID> activityIds);

    @Transactional
    void deleteById_ActivityId(UUID activityId);
}

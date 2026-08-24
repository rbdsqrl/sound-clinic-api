package com.simplehearing.activity.repository;

import com.simplehearing.activity.entity.ActivityInstruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ActivityInstructionRepository extends JpaRepository<ActivityInstruction, UUID> {
    List<ActivityInstruction> findByActivityIdOrderByOrderIndexAsc(UUID activityId);

    @Transactional
    void deleteByActivityId(UUID activityId);
}

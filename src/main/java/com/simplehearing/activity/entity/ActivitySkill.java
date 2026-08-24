package com.simplehearing.activity.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "activity_skills")
public class ActivitySkill {

    @EmbeddedId
    private ActivitySkillId id;

    public ActivitySkill() {}

    public ActivitySkill(UUID activityId, UUID skillId) {
        this.id = new ActivitySkillId(activityId, skillId);
    }

    public ActivitySkillId getId() { return id; }
    public UUID getActivityId() { return id.getActivityId(); }
    public UUID getSkillId() { return id.getSkillId(); }
}

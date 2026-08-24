package com.simplehearing.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ActivitySkillId implements Serializable {

    @Column(name = "activity_id")
    private UUID activityId;

    @Column(name = "skill_id")
    private UUID skillId;

    public ActivitySkillId() {}

    public ActivitySkillId(UUID activityId, UUID skillId) {
        this.activityId = activityId;
        this.skillId = skillId;
    }

    public UUID getActivityId() { return activityId; }
    public UUID getSkillId() { return skillId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivitySkillId)) return false;
        ActivitySkillId that = (ActivitySkillId) o;
        return Objects.equals(activityId, that.activityId) && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() { return Objects.hash(activityId, skillId); }
}

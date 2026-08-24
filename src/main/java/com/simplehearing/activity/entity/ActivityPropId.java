package com.simplehearing.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ActivityPropId implements Serializable {

    @Column(name = "activity_id")
    private UUID activityId;

    @Column(name = "prop_id")
    private UUID propId;

    public ActivityPropId() {}

    public ActivityPropId(UUID activityId, UUID propId) {
        this.activityId = activityId;
        this.propId = propId;
    }

    public UUID getActivityId() { return activityId; }
    public UUID getPropId() { return propId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityPropId)) return false;
        ActivityPropId that = (ActivityPropId) o;
        return Objects.equals(activityId, that.activityId) && Objects.equals(propId, that.propId);
    }

    @Override
    public int hashCode() { return Objects.hash(activityId, propId); }
}

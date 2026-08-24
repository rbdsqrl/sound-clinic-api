package com.simplehearing.activity.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "activity_props")
public class ActivityProp {

    @EmbeddedId
    private ActivityPropId id;

    public ActivityProp() {}

    public ActivityProp(UUID activityId, UUID propId) {
        this.id = new ActivityPropId(activityId, propId);
    }

    public ActivityPropId getId() { return id; }
    public UUID getActivityId() { return id.getActivityId(); }
    public UUID getPropId() { return id.getPropId(); }
}

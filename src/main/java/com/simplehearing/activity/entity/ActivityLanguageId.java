package com.simplehearing.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ActivityLanguageId implements Serializable {

    @Column(name = "activity_id")
    private UUID activityId;

    @Column(name = "language_id")
    private UUID languageId;

    public ActivityLanguageId() {}

    public ActivityLanguageId(UUID activityId, UUID languageId) {
        this.activityId = activityId;
        this.languageId = languageId;
    }

    public UUID getActivityId() { return activityId; }
    public UUID getLanguageId() { return languageId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityLanguageId)) return false;
        ActivityLanguageId that = (ActivityLanguageId) o;
        return Objects.equals(activityId, that.activityId) && Objects.equals(languageId, that.languageId);
    }

    @Override
    public int hashCode() { return Objects.hash(activityId, languageId); }
}

package com.simplehearing.activity.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "activity_languages")
public class ActivityLanguage {

    @EmbeddedId
    private ActivityLanguageId id;

    public ActivityLanguage() {}

    public ActivityLanguage(UUID activityId, UUID languageId) {
        this.id = new ActivityLanguageId(activityId, languageId);
    }

    public ActivityLanguageId getId() { return id; }
    public UUID getActivityId() { return id.getActivityId(); }
    public UUID getLanguageId() { return id.getLanguageId(); }
}

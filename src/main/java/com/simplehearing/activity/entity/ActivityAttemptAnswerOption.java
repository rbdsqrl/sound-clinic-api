package com.simplehearing.activity.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "activity_attempt_answer_options")
public class ActivityAttemptAnswerOption {

    @EmbeddedId
    private ActivityAttemptAnswerOptionId id;

    public ActivityAttemptAnswerOption() {}

    public ActivityAttemptAnswerOption(UUID answerId, UUID optionId) {
        this.id = new ActivityAttemptAnswerOptionId(answerId, optionId);
    }

    public ActivityAttemptAnswerOptionId getId() { return id; }
    public UUID getAnswerId() { return id.getAnswerId(); }
    public UUID getOptionId() { return id.getOptionId(); }
}

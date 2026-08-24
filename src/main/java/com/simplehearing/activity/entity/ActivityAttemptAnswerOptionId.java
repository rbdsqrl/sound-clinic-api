package com.simplehearing.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ActivityAttemptAnswerOptionId implements Serializable {

    @Column(name = "answer_id")
    private UUID answerId;

    @Column(name = "option_id")
    private UUID optionId;

    public ActivityAttemptAnswerOptionId() {}

    public ActivityAttemptAnswerOptionId(UUID answerId, UUID optionId) {
        this.answerId = answerId;
        this.optionId = optionId;
    }

    public UUID getAnswerId() { return answerId; }
    public UUID getOptionId() { return optionId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityAttemptAnswerOptionId)) return false;
        ActivityAttemptAnswerOptionId that = (ActivityAttemptAnswerOptionId) o;
        return Objects.equals(answerId, that.answerId) && Objects.equals(optionId, that.optionId);
    }

    @Override
    public int hashCode() { return Objects.hash(answerId, optionId); }
}

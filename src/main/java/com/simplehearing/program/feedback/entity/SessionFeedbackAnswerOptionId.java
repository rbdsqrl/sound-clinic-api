package com.simplehearing.program.feedback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SessionFeedbackAnswerOptionId implements Serializable {

    @Column(name = "answer_id")
    private UUID answerId;

    @Column(name = "option_id")
    private UUID optionId;

    public SessionFeedbackAnswerOptionId() {}

    public SessionFeedbackAnswerOptionId(UUID answerId, UUID optionId) {
        this.answerId = answerId;
        this.optionId = optionId;
    }

    public UUID getAnswerId() { return answerId; }
    public UUID getOptionId() { return optionId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionFeedbackAnswerOptionId)) return false;
        SessionFeedbackAnswerOptionId that = (SessionFeedbackAnswerOptionId) o;
        return Objects.equals(answerId, that.answerId) && Objects.equals(optionId, that.optionId);
    }

    @Override
    public int hashCode() { return Objects.hash(answerId, optionId); }
}

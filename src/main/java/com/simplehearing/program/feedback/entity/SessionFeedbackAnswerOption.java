package com.simplehearing.program.feedback.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "session_feedback_answer_options")
public class SessionFeedbackAnswerOption {

    @EmbeddedId
    private SessionFeedbackAnswerOptionId id;

    public SessionFeedbackAnswerOption() {}

    public SessionFeedbackAnswerOption(UUID answerId, UUID optionId) {
        this.id = new SessionFeedbackAnswerOptionId(answerId, optionId);
    }

    public SessionFeedbackAnswerOptionId getId() { return id; }
    public UUID getAnswerId() { return id.getAnswerId(); }
    public UUID getOptionId() { return id.getOptionId(); }
}

package com.simplehearing.activity.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "activity_attempt_answers")
public class ActivityAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "attempt_log_id", nullable = false)
    private UUID attemptLogId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "text_answer", columnDefinition = "TEXT")
    private String textAnswer;

    public ActivityAttemptAnswer() {}

    public UUID getId() { return id; }
    public UUID getAttemptLogId() { return attemptLogId; }
    public void setAttemptLogId(UUID attemptLogId) { this.attemptLogId = attemptLogId; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public String getTextAnswer() { return textAnswer; }
    public void setTextAnswer(String textAnswer) { this.textAnswer = textAnswer; }
}

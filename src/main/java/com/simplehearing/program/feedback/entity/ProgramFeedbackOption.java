package com.simplehearing.program.feedback.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "program_feedback_options")
public class ProgramFeedbackOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "option_text", nullable = false, length = 500)
    private String optionText;

    public ProgramFeedbackOption() {}

    public UUID getId() { return id; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
}

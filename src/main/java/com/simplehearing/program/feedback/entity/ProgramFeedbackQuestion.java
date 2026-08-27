package com.simplehearing.program.feedback.entity;

import com.simplehearing.program.feedback.enums.FeedbackQuestionType;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "program_feedback_questions")
public class ProgramFeedbackQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private FeedbackQuestionType questionType = FeedbackQuestionType.MULTI_CHOICE;

    public ProgramFeedbackQuestion() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public UUID getProgramId() { return programId; }
    public void setProgramId(UUID programId) { this.programId = programId; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public FeedbackQuestionType getQuestionType() { return questionType; }
    public void setQuestionType(FeedbackQuestionType questionType) { this.questionType = questionType; }
}

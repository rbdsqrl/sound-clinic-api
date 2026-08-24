package com.simplehearing.activity.entity;

import com.simplehearing.activity.enums.ChecklistQuestionType;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "activity_checklist_questions")
public class ActivityChecklistQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private ChecklistQuestionType questionType = ChecklistQuestionType.SINGLE_CHOICE;

    public ActivityChecklistQuestion() {}

    public UUID getId() { return id; }
    public UUID getActivityId() { return activityId; }
    public void setActivityId(UUID activityId) { this.activityId = activityId; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public ChecklistQuestionType getQuestionType() { return questionType; }
    public void setQuestionType(ChecklistQuestionType questionType) { this.questionType = questionType; }
}

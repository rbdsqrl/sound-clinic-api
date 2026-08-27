package com.simplehearing.assessment.entity;

import com.simplehearing.assessment.enums.AssessmentType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patient_assessments")
public class PatientAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", nullable = false, length = 20)
    private AssessmentType assessmentType;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Column(name = "filled_by", nullable = false)
    private UUID filledBy;

    /** JSON map of item number -> chosen score, e.g. {"1": 3, "2": 5}. */
    @Column(name = "item_scores", nullable = false, columnDefinition = "TEXT")
    private String itemScores;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(name = "classification", length = 30)
    private String classification;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PatientAssessment() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public AssessmentType getAssessmentType() { return assessmentType; }
    public void setAssessmentType(AssessmentType assessmentType) { this.assessmentType = assessmentType; }
    public LocalDate getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDate assessmentDate) { this.assessmentDate = assessmentDate; }
    public UUID getFilledBy() { return filledBy; }
    public void setFilledBy(UUID filledBy) { this.filledBy = filledBy; }
    public String getItemScores() { return itemScores; }
    public void setItemScores(String itemScores) { this.itemScores = itemScores; }
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }
    public Instant getCreatedAt() { return createdAt; }
}

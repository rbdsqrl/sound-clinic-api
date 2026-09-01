package com.simplehearing.assessment.entity;

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

    @Column(name = "definition_id", nullable = false)
    private UUID definitionId;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Column(name = "filled_by", nullable = false)
    private UUID filledBy;

    /** Null when the definition's scoringType is NONE (e.g. Pre Assessment Form). */
    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "classification", length = 60)
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
    public UUID getDefinitionId() { return definitionId; }
    public void setDefinitionId(UUID definitionId) { this.definitionId = definitionId; }
    public LocalDate getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDate assessmentDate) { this.assessmentDate = assessmentDate; }
    public UUID getFilledBy() { return filledBy; }
    public void setFilledBy(UUID filledBy) { this.filledBy = filledBy; }
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }
    public Instant getCreatedAt() { return createdAt; }
}

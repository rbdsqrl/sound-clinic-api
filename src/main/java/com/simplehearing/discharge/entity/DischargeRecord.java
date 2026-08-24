package com.simplehearing.discharge.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One row per discharge episode for a patient. A patient discharged and later re-enrolled gets
 * a second, independent DischargeRecord — the episode boundary is enforced by
 * {@code Enrollment.dischargedInRecordId}, not by date ranges.
 */
@Entity
@Table(name = "discharge_records")
public class DischargeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "discharge_date", nullable = false)
    private LocalDate dischargeDate;

    @Column(name = "discharged_by", nullable = false)
    private UUID dischargedBy;

    @Column(name = "episode_start_date")
    private LocalDate episodeStartDate;

    @Column(name = "final_assessment_snapshot", columnDefinition = "TEXT")
    private String finalAssessmentSnapshot;

    /** JSON — goal titles/domains/mastery frozen at discharge time. */
    @Column(name = "goals_at_discharge_snapshot", columnDefinition = "TEXT")
    private String goalsAtDischargeSnapshot;

    @Column(name = "avg_communication_rating")
    private BigDecimal avgCommunicationRating;

    @Column(name = "avg_progress_rating_pct")
    private BigDecimal avgProgressRatingPct;

    @Column(name = "goal_mastery_pct")
    private BigDecimal goalMasteryPct;

    @Column(name = "goal_mastery_met")
    private Boolean goalMasteryMet;

    @Column(name = "therapist_signoff_met", nullable = false)
    private boolean therapistSignoffMet;

    @Column(name = "parent_satisfaction_met")
    private Boolean parentSatisfactionMet;

    @Column(name = "overall_successful", nullable = false)
    private boolean overallSuccessful;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "pdf_url", length = 1000)
    private String pdfUrl;

    @Column(name = "pdf_generated_at")
    private Instant pdfGeneratedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DischargeRecord() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public LocalDate getDischargeDate() { return dischargeDate; }
    public void setDischargeDate(LocalDate dischargeDate) { this.dischargeDate = dischargeDate; }
    public UUID getDischargedBy() { return dischargedBy; }
    public void setDischargedBy(UUID dischargedBy) { this.dischargedBy = dischargedBy; }
    public LocalDate getEpisodeStartDate() { return episodeStartDate; }
    public void setEpisodeStartDate(LocalDate episodeStartDate) { this.episodeStartDate = episodeStartDate; }
    public String getFinalAssessmentSnapshot() { return finalAssessmentSnapshot; }
    public void setFinalAssessmentSnapshot(String finalAssessmentSnapshot) { this.finalAssessmentSnapshot = finalAssessmentSnapshot; }
    public String getGoalsAtDischargeSnapshot() { return goalsAtDischargeSnapshot; }
    public void setGoalsAtDischargeSnapshot(String goalsAtDischargeSnapshot) { this.goalsAtDischargeSnapshot = goalsAtDischargeSnapshot; }
    public BigDecimal getAvgCommunicationRating() { return avgCommunicationRating; }
    public void setAvgCommunicationRating(BigDecimal avgCommunicationRating) { this.avgCommunicationRating = avgCommunicationRating; }
    public BigDecimal getAvgProgressRatingPct() { return avgProgressRatingPct; }
    public void setAvgProgressRatingPct(BigDecimal avgProgressRatingPct) { this.avgProgressRatingPct = avgProgressRatingPct; }
    public BigDecimal getGoalMasteryPct() { return goalMasteryPct; }
    public void setGoalMasteryPct(BigDecimal goalMasteryPct) { this.goalMasteryPct = goalMasteryPct; }
    public Boolean getGoalMasteryMet() { return goalMasteryMet; }
    public void setGoalMasteryMet(Boolean goalMasteryMet) { this.goalMasteryMet = goalMasteryMet; }
    public boolean isTherapistSignoffMet() { return therapistSignoffMet; }
    public void setTherapistSignoffMet(boolean therapistSignoffMet) { this.therapistSignoffMet = therapistSignoffMet; }
    public Boolean getParentSatisfactionMet() { return parentSatisfactionMet; }
    public void setParentSatisfactionMet(Boolean parentSatisfactionMet) { this.parentSatisfactionMet = parentSatisfactionMet; }
    public boolean isOverallSuccessful() { return overallSuccessful; }
    public void setOverallSuccessful(boolean overallSuccessful) { this.overallSuccessful = overallSuccessful; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    public Instant getPdfGeneratedAt() { return pdfGeneratedAt; }
    public void setPdfGeneratedAt(Instant pdfGeneratedAt) { this.pdfGeneratedAt = pdfGeneratedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

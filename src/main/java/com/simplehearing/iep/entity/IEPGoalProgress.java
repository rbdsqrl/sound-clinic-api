package com.simplehearing.iep.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "iep_goal_progress")
public class IEPGoalProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @Column(name = "therapist_id", nullable = false)
    private UUID therapistId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "trials_passed")
    private Integer trialsPassed;

    @Column(name = "trials_total")
    private Integer trialsTotal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public IEPGoalProgress() {}

    public UUID getId() { return id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public UUID getGoalId() { return goalId; }
    public void setGoalId(UUID goalId) { this.goalId = goalId; }

    public UUID getTherapistId() { return therapistId; }
    public void setTherapistId(UUID therapistId) { this.therapistId = therapistId; }

    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Integer getTrialsPassed() { return trialsPassed; }
    public void setTrialsPassed(Integer trialsPassed) { this.trialsPassed = trialsPassed; }

    public Integer getTrialsTotal() { return trialsTotal; }
    public void setTrialsTotal(Integer trialsTotal) { this.trialsTotal = trialsTotal; }

    public Instant getCreatedAt() { return createdAt; }
}

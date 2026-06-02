package com.simplehearing.iep.entity;

import com.simplehearing.iep.enums.IEPGoalDomain;
import com.simplehearing.iep.enums.IEPGoalStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "iep_goals")
public class IEPGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "goal_statement", columnDefinition = "TEXT")
    private String goalStatement;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false)
    private IEPGoalDomain domain;

    @Column(name = "baseline", columnDefinition = "TEXT")
    private String baseline;

    @Column(name = "target_criteria", length = 500)
    private String targetCriteria;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IEPGoalStatus status = IEPGoalStatus.IN_PROGRESS;

    @Column(name = "progress_tag", length = 2)
    private String progressTag;

    @Column(name = "assigned_therapist_id")
    private UUID assignedTherapistId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public IEPGoal() {}

    public UUID getId() { return id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGoalStatement() { return goalStatement; }
    public void setGoalStatement(String goalStatement) { this.goalStatement = goalStatement; }

    public IEPGoalDomain getDomain() { return domain; }
    public void setDomain(IEPGoalDomain domain) { this.domain = domain; }

    public String getBaseline() { return baseline; }
    public void setBaseline(String baseline) { this.baseline = baseline; }

    public String getTargetCriteria() { return targetCriteria; }
    public void setTargetCriteria(String targetCriteria) { this.targetCriteria = targetCriteria; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public IEPGoalStatus getStatus() { return status; }
    public void setStatus(IEPGoalStatus status) { this.status = status; }

    public String getProgressTag() { return progressTag; }
    public void setProgressTag(String progressTag) { this.progressTag = progressTag; }

    public UUID getAssignedTherapistId() { return assignedTherapistId; }
    public void setAssignedTherapistId(UUID assignedTherapistId) { this.assignedTherapistId = assignedTherapistId; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

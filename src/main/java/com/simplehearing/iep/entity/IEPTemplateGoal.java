package com.simplehearing.iep.entity;

import com.simplehearing.iep.enums.IEPGoalDomain;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iep_template_goals")
public class IEPTemplateGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "goal_statement", columnDefinition = "TEXT")
    private String goalStatement;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain", length = 50)
    private IEPGoalDomain domain;

    @Column(name = "baseline", columnDefinition = "TEXT")
    private String baseline;

    @Column(name = "target_criteria", columnDefinition = "TEXT")
    private String targetCriteria;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public IEPTemplateGoal() {}

    public UUID getId() { return id; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

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

    public Instant getCreatedAt() { return createdAt; }
}

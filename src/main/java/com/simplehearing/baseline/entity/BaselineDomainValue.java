package com.simplehearing.baseline.entity;

import com.simplehearing.baseline.enums.BaselineDomain;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "baseline_domain_values")
public class BaselineDomainValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false)
    private BaselineDomain domain;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    /** Optional 0-100 score alongside the free-text value — lets a domain be charted when a
     *  clinician chooses to score it, without forcing every domain into a number. */
    @Column(name = "score_percent")
    private Integer scorePercent;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BaselineDomainValue() {}

    public UUID getId() { return id; }

    public UUID getReportId() { return reportId; }
    public void setReportId(UUID reportId) { this.reportId = reportId; }

    public BaselineDomain getDomain() { return domain; }
    public void setDomain(BaselineDomain domain) { this.domain = domain; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Integer getScorePercent() { return scorePercent; }
    public void setScorePercent(Integer scorePercent) { this.scorePercent = scorePercent; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public Instant getUpdatedAt() { return updatedAt; }
}

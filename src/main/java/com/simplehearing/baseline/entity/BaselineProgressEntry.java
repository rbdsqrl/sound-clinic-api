package com.simplehearing.baseline.entity;

import com.simplehearing.baseline.enums.BaselineDomain;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "baseline_progress_entries")
public class BaselineProgressEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false)
    private BaselineDomain domain;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    /** Optional 0-100 score alongside the free-text value — lets progress be charted when a
     *  clinician chooses to score this entry. */
    @Column(name = "score_percent")
    private Integer scorePercent;

    @Column(name = "logged_by", nullable = false)
    private UUID loggedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public BaselineProgressEntry() {}

    public UUID getId() { return id; }

    public UUID getReportId() { return reportId; }
    public void setReportId(UUID reportId) { this.reportId = reportId; }

    public BaselineDomain getDomain() { return domain; }
    public void setDomain(BaselineDomain domain) { this.domain = domain; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Integer getScorePercent() { return scorePercent; }
    public void setScorePercent(Integer scorePercent) { this.scorePercent = scorePercent; }

    public UUID getLoggedBy() { return loggedBy; }
    public void setLoggedBy(UUID loggedBy) { this.loggedBy = loggedBy; }

    public Instant getCreatedAt() { return createdAt; }
}

package com.simplehearing.leave.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A kind of leave (Casual, Sick…) with an optional yearly quota. Deactivated, never deleted, so past leaves keep their label. */
@Entity
@Table(name = "leave_categories")
public class LeaveCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false, length = 80)
    private String name;

    /** Days per leave year each person gets by default; null = no limit. */
    @Column(name = "annual_days")
    private Integer annualDays;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LeaveCategory() {}

    public UUID getId()                       { return id; }
    public UUID getOrgId()                    { return orgId; }
    public void setOrgId(UUID v)              { this.orgId = v; }
    public String getName()                   { return name; }
    public void setName(String v)             { this.name = v; }
    public Integer getAnnualDays()            { return annualDays; }
    public void setAnnualDays(Integer v)      { this.annualDays = v; }
    public boolean isActive()                 { return isActive; }
    public void setActive(boolean v)          { this.isActive = v; }
    public Instant getCreatedAt()             { return createdAt; }
}

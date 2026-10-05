package com.simplehearing.leave.entity;

import jakarta.persistence.*;

import java.util.UUID;

/** One person's own allocation for a category in a leave year — overrides the category's default. */
@Entity
@Table(name = "leave_allocations")
public class LeaveAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    /** The calendar year the leave year STARTS in (an April-March year 2026-27 is 2026). */
    @Column(name = "leave_year", nullable = false)
    private int leaveYear;

    @Column(nullable = false)
    private int days;

    public LeaveAllocation() {}

    public UUID getId()                     { return id; }
    public UUID getOrgId()                  { return orgId; }
    public void setOrgId(UUID v)            { this.orgId = v; }
    public UUID getUserId()                 { return userId; }
    public void setUserId(UUID v)           { this.userId = v; }
    public UUID getCategoryId()             { return categoryId; }
    public void setCategoryId(UUID v)       { this.categoryId = v; }
    public int getLeaveYear()               { return leaveYear; }
    public void setLeaveYear(int v)         { this.leaveYear = v; }
    public int getDays()                    { return days; }
    public void setDays(int v)              { this.days = v; }
}

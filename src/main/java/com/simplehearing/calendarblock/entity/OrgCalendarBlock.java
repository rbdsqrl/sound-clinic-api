package com.simplehearing.calendarblock.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** An org-wide recurring calendar block (e.g. "Lunch Break") — a rule, not materialized rows.
 *  The calendar expands it on the fly for whatever date range is being viewed, and it is shown on
 *  every authenticated user's calendar automatically. Unlike a Meeting it has no participant list
 *  to maintain, and unlike a Public Holiday it does not block session/review-meeting
 *  autoscheduling — it is a shared visibility marker, not a day off. */
@Entity
@Table(name = "org_calendar_blocks")
public class OrgCalendarBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "org_calendar_block_days", joinColumns = @JoinColumn(name = "org_calendar_block_id"))
    @Column(name = "day_of_week", length = 10)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** NULL = ongoing indefinitely. */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OrgCalendarBlock() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Set<DayOfWeek> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.simplehearing.activity.entity;

import com.simplehearing.activity.enums.ActivityDifficulty;
import com.simplehearing.activity.enums.AgeUnit;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "about_activity", nullable = false, columnDefinition = "TEXT")
    private String aboutActivity;

    @Column(name = "therapy_id")
    private UUID therapyId;

    @Column(name = "duration_weeks", nullable = false)
    private Integer durationWeeks;

    @Column(name = "age_min_value", nullable = false)
    private Integer ageMinValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_min_unit", nullable = false, length = 10)
    private AgeUnit ageMinUnit = AgeUnit.YEAR;

    @Column(name = "age_max_value", nullable = false)
    private Integer ageMaxValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_max_unit", nullable = false, length = 10)
    private AgeUnit ageMaxUnit = AgeUnit.YEAR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActivityDifficulty difficulty = ActivityDifficulty.EASY;

    @Column(name = "tips_and_suggestions", columnDefinition = "TEXT")
    private String tipsAndSuggestions;

    @Column(name = "is_shared", nullable = false)
    private boolean isShared = false;

    @Column(name = "source_activity_id")
    private UUID sourceActivityId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Activity() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAboutActivity() { return aboutActivity; }
    public void setAboutActivity(String aboutActivity) { this.aboutActivity = aboutActivity; }
    public UUID getTherapyId() { return therapyId; }
    public void setTherapyId(UUID therapyId) { this.therapyId = therapyId; }
    public Integer getDurationWeeks() { return durationWeeks; }
    public void setDurationWeeks(Integer durationWeeks) { this.durationWeeks = durationWeeks; }
    public Integer getAgeMinValue() { return ageMinValue; }
    public void setAgeMinValue(Integer ageMinValue) { this.ageMinValue = ageMinValue; }
    public AgeUnit getAgeMinUnit() { return ageMinUnit; }
    public void setAgeMinUnit(AgeUnit ageMinUnit) { this.ageMinUnit = ageMinUnit; }
    public Integer getAgeMaxValue() { return ageMaxValue; }
    public void setAgeMaxValue(Integer ageMaxValue) { this.ageMaxValue = ageMaxValue; }
    public AgeUnit getAgeMaxUnit() { return ageMaxUnit; }
    public void setAgeMaxUnit(AgeUnit ageMaxUnit) { this.ageMaxUnit = ageMaxUnit; }
    public ActivityDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(ActivityDifficulty difficulty) { this.difficulty = difficulty; }
    public String getTipsAndSuggestions() { return tipsAndSuggestions; }
    public void setTipsAndSuggestions(String tipsAndSuggestions) { this.tipsAndSuggestions = tipsAndSuggestions; }
    public boolean isShared() { return isShared; }
    public void setShared(boolean shared) { this.isShared = shared; }
    public UUID getSourceActivityId() { return sourceActivityId; }
    public void setSourceActivityId(UUID sourceActivityId) { this.sourceActivityId = sourceActivityId; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

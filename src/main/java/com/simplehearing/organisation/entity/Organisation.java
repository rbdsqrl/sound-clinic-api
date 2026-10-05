package com.simplehearing.organisation.entity;

import com.simplehearing.organisation.enums.AiProvider;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "organisations")
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    /** URL-safe identifier, e.g. "city-hearing". Unique across all orgs. */
    @Column(unique = true, nullable = false)
    private String slug;

    private String contactEmail;
    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String logoUrl;

    @Column(nullable = false)
    private String timezone = "UTC";

    /** Geo-fence for BUSINESS_OWNER attendance check-in — verified against the org's own
     *  registered address rather than any single clinic (mirrors {@code Clinic}'s fields). */
    private Double latitude;

    private Double longitude;

    @Column(name = "geo_fence_radius_meters")
    private Integer geoFenceRadiusMeters = 200;

    @Column(nullable = false)
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_provider", length = 20)
    private AiProvider aiProvider;

    /** Never serialised back to the frontend — see {@code OrganisationResponse}. */
    @Column(name = "ai_api_key", length = 500)
    private String aiApiKey;

    /** Discharge success-criteria thresholds — editable later via an org-settings screen. */
    @Column(name = "goal_mastery_threshold_pct", nullable = false)
    private int goalMasteryThresholdPct = 90;

    @Column(name = "parent_satisfaction_threshold_pct", nullable = false)
    private int parentSatisfactionThresholdPct = 70;

    @Column(name = "require_all_enrollments_for_discharge", nullable = false)
    private boolean requireAllEnrollmentsForDischarge = true;

    /** Goal video evidence rules. 0 videos required = evidence is optional. */
    @Column(name = "evidence_videos_required", nullable = false)
    private int evidenceVideosRequired = 1;

    @Column(name = "evidence_max_video_mb", nullable = false)
    private int evidenceMaxVideoMb = 50;

    @Column(name = "evidence_max_video_seconds", nullable = false)
    private int evidenceMaxVideoSeconds = 120;

    /** Days of the week autoscheduling (therapy sessions, review meetings) always skips — same treatment as public holidays. Ad-hoc sessions are unaffected. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "organisation_weekly_off_days", joinColumns = @JoinColumn(name = "organisation_id"))
    @Column(name = "day_of_week", length = 10)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> weeklyOffDays = EnumSet.noneOf(DayOfWeek.class);

    /** The org-wide default daily grid a Review Meeting is booked into (see ReviewMeetingService)
     *  — an org-configurable list of times (as many as they want), editable the same way as
     *  weeklyOffDays. A Clinic Head with their own grid (User.reviewSlotTimes) uses that instead. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "organisation_review_slot_times", joinColumns = @JoinColumn(name = "organisation_id"))
    @Column(name = "slot_time", nullable = false)
    private Set<LocalTime> reviewSlotTimes = defaultReviewSlotTimes();

    private static Set<LocalTime> defaultReviewSlotTimes() {
        return new LinkedHashSet<>(List.of(
                LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0),
                LocalTime.of(15, 0), LocalTime.of(16, 0), LocalTime.of(17, 0)));
    }

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public Organisation() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getGeoFenceRadiusMeters() { return geoFenceRadiusMeters; }
    public void setGeoFenceRadiusMeters(Integer geoFenceRadiusMeters) { this.geoFenceRadiusMeters = geoFenceRadiusMeters; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public AiProvider getAiProvider() { return aiProvider; }
    public void setAiProvider(AiProvider aiProvider) { this.aiProvider = aiProvider; }

    public String getAiApiKey() { return aiApiKey; }
    public void setAiApiKey(String aiApiKey) { this.aiApiKey = aiApiKey; }

    public int getEvidenceVideosRequired() { return evidenceVideosRequired; }
    public void setEvidenceVideosRequired(int v) { this.evidenceVideosRequired = v; }
    public int getEvidenceMaxVideoMb() { return evidenceMaxVideoMb; }
    public void setEvidenceMaxVideoMb(int v) { this.evidenceMaxVideoMb = v; }
    public int getEvidenceMaxVideoSeconds() { return evidenceMaxVideoSeconds; }
    public void setEvidenceMaxVideoSeconds(int v) { this.evidenceMaxVideoSeconds = v; }
    public int getGoalMasteryThresholdPct() { return goalMasteryThresholdPct; }
    public void setGoalMasteryThresholdPct(int goalMasteryThresholdPct) { this.goalMasteryThresholdPct = goalMasteryThresholdPct; }

    public int getParentSatisfactionThresholdPct() { return parentSatisfactionThresholdPct; }
    public void setParentSatisfactionThresholdPct(int parentSatisfactionThresholdPct) { this.parentSatisfactionThresholdPct = parentSatisfactionThresholdPct; }

    public boolean isRequireAllEnrollmentsForDischarge() { return requireAllEnrollmentsForDischarge; }
    public void setRequireAllEnrollmentsForDischarge(boolean requireAllEnrollmentsForDischarge) { this.requireAllEnrollmentsForDischarge = requireAllEnrollmentsForDischarge; }

    public Set<DayOfWeek> getWeeklyOffDays() { return weeklyOffDays; }
    public void setWeeklyOffDays(Set<DayOfWeek> weeklyOffDays) { this.weeklyOffDays = weeklyOffDays; }

    public Set<LocalTime> getReviewSlotTimes() { return reviewSlotTimes; }
    public void setReviewSlotTimes(Set<LocalTime> reviewSlotTimes) { this.reviewSlotTimes = reviewSlotTimes; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

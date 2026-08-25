package com.simplehearing.casehistory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row per patient — a single clinical intake record, edited in place. Checkbox-group
 * selections, the fixed milestone-skills list, and the family-members list are stored as
 * JSON text (see column comments in 073-create-case-histories.sql).
 */
@Entity
@Table(name = "case_histories")
public class CaseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "patient_id", nullable = false, unique = true)
    private UUID patientId;

    // ── Basic Concerns ──────────────────────────────────────────────────────
    @Column(name = "present_complaints", columnDefinition = "TEXT")
    private String presentComplaints;

    /** JSON array of selected habit labels. */
    @Column(columnDefinition = "TEXT")
    private String habits;

    @Column(name = "physical_other_problems", columnDefinition = "TEXT")
    private String physicalOtherProblems;

    // ── Birth History ────────────────────────────────────────────────────────
    /** JSON array of selected prenatal-health labels. */
    @Column(name = "prenatal_health", columnDefinition = "TEXT")
    private String prenatalHealth;

    @Column(name = "delivery_type")
    private String deliveryType;

    @Column(name = "labour_type")
    private String labourType;

    @Column(name = "birth_cry")
    private String birthCry;

    @Column(name = "prenatal_notes", columnDefinition = "TEXT")
    private String prenatalNotes;

    @Column(name = "birth_additional_notes", columnDefinition = "TEXT")
    private String birthAdditionalNotes;

    @Column(name = "birth_height")
    private BigDecimal birthHeight;

    @Column(name = "birth_height_unit")
    private String birthHeightUnit;

    @Column(name = "birth_weight")
    private BigDecimal birthWeight;

    @Column(name = "birth_weight_unit")
    private String birthWeightUnit;

    /** JSON array of selected postnatal-health labels. */
    @Column(name = "postnatal_health", columnDefinition = "TEXT")
    private String postnatalHealth;

    @Column(name = "phototherapy_days")
    private Integer phototherapyDays;

    @Column(name = "postnatal_notes", columnDefinition = "TEXT")
    private String postnatalNotes;

    // ── Milestones ──────────────────────────────────────────────────────────
    @Column(name = "motor_milestones")
    private String motorMilestones;

    @Column(name = "speech_milestones")
    private String speechMilestones;

    /** JSON array of {skill, notPresent, unaware, ageInMonths, status}. */
    @Column(name = "milestone_skills", columnDefinition = "TEXT")
    private String milestoneSkills;

    @Column(name = "milestones_additional_notes", columnDefinition = "TEXT")
    private String milestonesAdditionalNotes;

    private String handedness;

    // ── Family History ──────────────────────────────────────────────────────
    @Column(name = "family_type")
    private String familyType;

    /** JSON array of {name, relation, age, notes}. */
    @Column(name = "family_members", columnDefinition = "TEXT")
    private String familyMembers;

    @Column(name = "consanguinity_history")
    private Boolean consanguinityHistory;

    @Column(name = "family_impairments_notes", columnDefinition = "TEXT")
    private String familyImpairmentsNotes;

    // ── Social & Behavior History ───────────────────────────────────────────
    @Column(name = "eye_contact")
    private String eyeContact;

    @Column(name = "stuttering_frequency")
    private String stutteringFrequency;

    @Column(name = "play_behavior")
    private String playBehavior;

    @Column(name = "social_smiling")
    private String socialSmiling;

    @Column(name = "behavioural_self_regulation")
    private String behaviouralSelfRegulation;

    @Column(name = "emotional_self_regulation")
    private String emotionalSelfRegulation;

    private String friendships;

    private String listening;

    /** JSON array of selected communication labels. */
    @Column(columnDefinition = "TEXT")
    private String communications;

    /** JSON array of selected behavioral-problem labels. */
    @Column(name = "behavioral_problems", columnDefinition = "TEXT")
    private String behavioralProblems;

    @Column(name = "provisional_diagnosis", columnDefinition = "TEXT")
    private String provisionalDiagnosis;

    // ── School History ──────────────────────────────────────────────────────
    @Column(name = "current_grade")
    private String currentGrade;

    private String school;

    private String syllabus;

    @Column(name = "age_of_joining")
    private BigDecimal ageOfJoining;

    @Column(name = "performance_and_progress", columnDefinition = "TEXT")
    private String performanceAndProgress;

    @Column(name = "attitude_towards_studies", columnDefinition = "TEXT")
    private String attitudeTowardsStudies;

    @Column(name = "school_additional_notes", columnDefinition = "TEXT")
    private String schoolAdditionalNotes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CaseHistory() {}

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public String getPresentComplaints() { return presentComplaints; }
    public void setPresentComplaints(String presentComplaints) { this.presentComplaints = presentComplaints; }
    public String getHabits() { return habits; }
    public void setHabits(String habits) { this.habits = habits; }
    public String getPhysicalOtherProblems() { return physicalOtherProblems; }
    public void setPhysicalOtherProblems(String physicalOtherProblems) { this.physicalOtherProblems = physicalOtherProblems; }

    public String getPrenatalHealth() { return prenatalHealth; }
    public void setPrenatalHealth(String prenatalHealth) { this.prenatalHealth = prenatalHealth; }
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
    public String getLabourType() { return labourType; }
    public void setLabourType(String labourType) { this.labourType = labourType; }
    public String getBirthCry() { return birthCry; }
    public void setBirthCry(String birthCry) { this.birthCry = birthCry; }
    public String getPrenatalNotes() { return prenatalNotes; }
    public void setPrenatalNotes(String prenatalNotes) { this.prenatalNotes = prenatalNotes; }
    public String getBirthAdditionalNotes() { return birthAdditionalNotes; }
    public void setBirthAdditionalNotes(String birthAdditionalNotes) { this.birthAdditionalNotes = birthAdditionalNotes; }
    public BigDecimal getBirthHeight() { return birthHeight; }
    public void setBirthHeight(BigDecimal birthHeight) { this.birthHeight = birthHeight; }
    public String getBirthHeightUnit() { return birthHeightUnit; }
    public void setBirthHeightUnit(String birthHeightUnit) { this.birthHeightUnit = birthHeightUnit; }
    public BigDecimal getBirthWeight() { return birthWeight; }
    public void setBirthWeight(BigDecimal birthWeight) { this.birthWeight = birthWeight; }
    public String getBirthWeightUnit() { return birthWeightUnit; }
    public void setBirthWeightUnit(String birthWeightUnit) { this.birthWeightUnit = birthWeightUnit; }
    public String getPostnatalHealth() { return postnatalHealth; }
    public void setPostnatalHealth(String postnatalHealth) { this.postnatalHealth = postnatalHealth; }
    public Integer getPhototherapyDays() { return phototherapyDays; }
    public void setPhototherapyDays(Integer phototherapyDays) { this.phototherapyDays = phototherapyDays; }
    public String getPostnatalNotes() { return postnatalNotes; }
    public void setPostnatalNotes(String postnatalNotes) { this.postnatalNotes = postnatalNotes; }

    public String getMotorMilestones() { return motorMilestones; }
    public void setMotorMilestones(String motorMilestones) { this.motorMilestones = motorMilestones; }
    public String getSpeechMilestones() { return speechMilestones; }
    public void setSpeechMilestones(String speechMilestones) { this.speechMilestones = speechMilestones; }
    public String getMilestoneSkills() { return milestoneSkills; }
    public void setMilestoneSkills(String milestoneSkills) { this.milestoneSkills = milestoneSkills; }
    public String getMilestonesAdditionalNotes() { return milestonesAdditionalNotes; }
    public void setMilestonesAdditionalNotes(String milestonesAdditionalNotes) { this.milestonesAdditionalNotes = milestonesAdditionalNotes; }
    public String getHandedness() { return handedness; }
    public void setHandedness(String handedness) { this.handedness = handedness; }

    public String getFamilyType() { return familyType; }
    public void setFamilyType(String familyType) { this.familyType = familyType; }
    public String getFamilyMembers() { return familyMembers; }
    public void setFamilyMembers(String familyMembers) { this.familyMembers = familyMembers; }
    public Boolean getConsanguinityHistory() { return consanguinityHistory; }
    public void setConsanguinityHistory(Boolean consanguinityHistory) { this.consanguinityHistory = consanguinityHistory; }
    public String getFamilyImpairmentsNotes() { return familyImpairmentsNotes; }
    public void setFamilyImpairmentsNotes(String familyImpairmentsNotes) { this.familyImpairmentsNotes = familyImpairmentsNotes; }

    public String getEyeContact() { return eyeContact; }
    public void setEyeContact(String eyeContact) { this.eyeContact = eyeContact; }
    public String getStutteringFrequency() { return stutteringFrequency; }
    public void setStutteringFrequency(String stutteringFrequency) { this.stutteringFrequency = stutteringFrequency; }
    public String getPlayBehavior() { return playBehavior; }
    public void setPlayBehavior(String playBehavior) { this.playBehavior = playBehavior; }
    public String getSocialSmiling() { return socialSmiling; }
    public void setSocialSmiling(String socialSmiling) { this.socialSmiling = socialSmiling; }
    public String getBehaviouralSelfRegulation() { return behaviouralSelfRegulation; }
    public void setBehaviouralSelfRegulation(String behaviouralSelfRegulation) { this.behaviouralSelfRegulation = behaviouralSelfRegulation; }
    public String getEmotionalSelfRegulation() { return emotionalSelfRegulation; }
    public void setEmotionalSelfRegulation(String emotionalSelfRegulation) { this.emotionalSelfRegulation = emotionalSelfRegulation; }
    public String getFriendships() { return friendships; }
    public void setFriendships(String friendships) { this.friendships = friendships; }
    public String getListening() { return listening; }
    public void setListening(String listening) { this.listening = listening; }
    public String getCommunications() { return communications; }
    public void setCommunications(String communications) { this.communications = communications; }
    public String getBehavioralProblems() { return behavioralProblems; }
    public void setBehavioralProblems(String behavioralProblems) { this.behavioralProblems = behavioralProblems; }
    public String getProvisionalDiagnosis() { return provisionalDiagnosis; }
    public void setProvisionalDiagnosis(String provisionalDiagnosis) { this.provisionalDiagnosis = provisionalDiagnosis; }

    public String getCurrentGrade() { return currentGrade; }
    public void setCurrentGrade(String currentGrade) { this.currentGrade = currentGrade; }
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public String getSyllabus() { return syllabus; }
    public void setSyllabus(String syllabus) { this.syllabus = syllabus; }
    public BigDecimal getAgeOfJoining() { return ageOfJoining; }
    public void setAgeOfJoining(BigDecimal ageOfJoining) { this.ageOfJoining = ageOfJoining; }
    public String getPerformanceAndProgress() { return performanceAndProgress; }
    public void setPerformanceAndProgress(String performanceAndProgress) { this.performanceAndProgress = performanceAndProgress; }
    public String getAttitudeTowardsStudies() { return attitudeTowardsStudies; }
    public void setAttitudeTowardsStudies(String attitudeTowardsStudies) { this.attitudeTowardsStudies = attitudeTowardsStudies; }
    public String getSchoolAdditionalNotes() { return schoolAdditionalNotes; }
    public void setSchoolAdditionalNotes(String schoolAdditionalNotes) { this.schoolAdditionalNotes = schoolAdditionalNotes; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

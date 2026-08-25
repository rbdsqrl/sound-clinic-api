package com.simplehearing.casehistory.dto;

import java.math.BigDecimal;
import java.util.List;

/** Covers the whole form — the UI saves every section in one request, matching the legacy single Save button. */
public record UpdateCaseHistoryRequest(
        String presentComplaints,
        List<String> habits,
        String physicalOtherProblems,

        List<String> prenatalHealth,
        String deliveryType,
        String labourType,
        String birthCry,
        String prenatalNotes,
        String birthAdditionalNotes,
        BigDecimal birthHeight,
        String birthHeightUnit,
        BigDecimal birthWeight,
        String birthWeightUnit,
        List<String> postnatalHealth,
        Integer phototherapyDays,
        String postnatalNotes,

        String motorMilestones,
        String speechMilestones,
        List<CaseHistoryResponse.MilestoneSkill> milestoneSkills,
        String milestonesAdditionalNotes,
        String handedness,

        String familyType,
        List<CaseHistoryResponse.FamilyMember> familyMembers,
        Boolean consanguinityHistory,
        String familyImpairmentsNotes,

        String eyeContact,
        String stutteringFrequency,
        String playBehavior,
        String socialSmiling,
        String behaviouralSelfRegulation,
        String emotionalSelfRegulation,
        String friendships,
        String listening,
        List<String> communications,
        List<String> behavioralProblems,
        String provisionalDiagnosis,

        String currentGrade,
        String school,
        String syllabus,
        BigDecimal ageOfJoining,
        String performanceAndProgress,
        String attitudeTowardsStudies,
        String schoolAdditionalNotes
) {}

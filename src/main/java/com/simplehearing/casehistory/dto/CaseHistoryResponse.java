package com.simplehearing.casehistory.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplehearing.casehistory.entity.CaseHistory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CaseHistoryResponse(
        UUID id,
        UUID patientId,

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
        List<MilestoneSkill> milestoneSkills,
        String milestonesAdditionalNotes,
        String handedness,

        String familyType,
        List<FamilyMember> familyMembers,
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
        String schoolAdditionalNotes,

        Instant createdAt,
        Instant updatedAt
) {
    public record MilestoneSkill(String skill, boolean notPresent, boolean unaware, BigDecimal ageInMonths, String status) {}

    public record FamilyMember(String name, String relation, String age, String notes) {}

    public static CaseHistoryResponse from(CaseHistory c, ObjectMapper mapper) {
        return new CaseHistoryResponse(
                c.getId(),
                c.getPatientId(),
                c.getPresentComplaints(),
                readStringList(mapper, c.getHabits()),
                c.getPhysicalOtherProblems(),
                readStringList(mapper, c.getPrenatalHealth()),
                c.getDeliveryType(),
                c.getLabourType(),
                c.getBirthCry(),
                c.getPrenatalNotes(),
                c.getBirthAdditionalNotes(),
                c.getBirthHeight(),
                c.getBirthHeightUnit(),
                c.getBirthWeight(),
                c.getBirthWeightUnit(),
                readStringList(mapper, c.getPostnatalHealth()),
                c.getPhototherapyDays(),
                c.getPostnatalNotes(),
                c.getMotorMilestones(),
                c.getSpeechMilestones(),
                readList(mapper, c.getMilestoneSkills(), new TypeReference<List<MilestoneSkill>>() {}),
                c.getMilestonesAdditionalNotes(),
                c.getHandedness(),
                c.getFamilyType(),
                readList(mapper, c.getFamilyMembers(), new TypeReference<List<FamilyMember>>() {}),
                c.getConsanguinityHistory(),
                c.getFamilyImpairmentsNotes(),
                c.getEyeContact(),
                c.getStutteringFrequency(),
                c.getPlayBehavior(),
                c.getSocialSmiling(),
                c.getBehaviouralSelfRegulation(),
                c.getEmotionalSelfRegulation(),
                c.getFriendships(),
                c.getListening(),
                readStringList(mapper, c.getCommunications()),
                readStringList(mapper, c.getBehavioralProblems()),
                c.getProvisionalDiagnosis(),
                c.getCurrentGrade(),
                c.getSchool(),
                c.getSyllabus(),
                c.getAgeOfJoining(),
                c.getPerformanceAndProgress(),
                c.getAttitudeTowardsStudies(),
                c.getSchoolAdditionalNotes(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private static List<String> readStringList(ObjectMapper mapper, String json) {
        return readList(mapper, json, new TypeReference<List<String>>() {});
    }

    private static <T> List<T> readList(ObjectMapper mapper, String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            return List.of();
        }
    }
}

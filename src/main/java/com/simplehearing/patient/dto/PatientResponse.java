package com.simplehearing.patient.dto;

import com.simplehearing.condition.entity.Condition;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.entity.PatientCondition;
import com.simplehearing.patient.entity.TherapistPatient;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        UUID orgId,
        UUID clinicId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        String notes,
        boolean isActive,
        Instant createdAt,
        List<ConditionSummary> conditions,
        List<ParentSummary> parents,
        List<TherapistSummary> therapists
) {
    public record ConditionSummary(UUID id, String name, LocalDate diagnosedAt, String notes) {}
    public record ParentSummary(UUID id, String firstName, String lastName, String email) {}
    public record TherapistSummary(UUID id, String firstName, String lastName, Instant assignedAt) {}

    public static PatientResponse from(Patient patient,
                                       List<PatientCondition> conditions,
                                       List<Condition> conditionDetails,
                                       List<User> parents,
                                       List<TherapistPatient> therapistAssignments,
                                       List<User> therapists) {

        List<ConditionSummary> conditionSummaries = conditions.stream().map(pc -> {
            Condition c = conditionDetails.stream()
                    .filter(d -> d.getId().equals(pc.getId().getConditionId()))
                    .findFirst().orElse(null);
            return new ConditionSummary(
                    pc.getId().getConditionId(),
                    c != null ? c.getName() : "Unknown",
                    pc.getDiagnosedAt(),
                    pc.getNotes()
            );
        }).toList();

        List<ParentSummary> parentSummaries = parents.stream()
                .map(u -> new ParentSummary(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail()))
                .toList();

        List<TherapistSummary> therapistSummaries = therapistAssignments.stream().map(ta -> {
            User t = therapists.stream()
                    .filter(u -> u.getId().equals(ta.getTherapistId()))
                    .findFirst().orElse(null);
            return new TherapistSummary(
                    ta.getTherapistId(),
                    t != null ? t.getFirstName() : "Unknown",
                    t != null ? t.getLastName() : "",
                    ta.getAssignedAt()
            );
        }).toList();

        return new PatientResponse(
                patient.getId(), patient.getOrgId(), patient.getClinicId(),
                patient.getFirstName(), patient.getLastName(),
                patient.getDateOfBirth(), patient.getGender(), patient.getNotes(),
                patient.isActive(), patient.getCreatedAt(),
                conditionSummaries, parentSummaries, therapistSummaries
        );
    }
}

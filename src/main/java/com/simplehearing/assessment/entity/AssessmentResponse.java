package com.simplehearing.assessment.entity;

import jakarta.persistence.*;

import java.util.UUID;

/** One answered item within a {@link PatientAssessment} fill. */
@Entity
@Table(name = "patient_assessment_responses")
public class AssessmentResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_assessment_id", nullable = false)
    private UUID patientAssessmentId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    /** SINGLE_SELECT answer. */
    @Column(name = "selected_option_id")
    private UUID selectedOptionId;

    /** TEXT/FILE answer, or a JSON array of option ids for MULTI_SELECT. */
    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    public AssessmentResponse() {}

    public UUID getId()                             { return id; }
    public UUID getPatientAssessmentId()             { return patientAssessmentId; }
    public void setPatientAssessmentId(UUID v)       { this.patientAssessmentId = v; }
    public UUID getItemId()                          { return itemId; }
    public void setItemId(UUID v)                    { this.itemId = v; }
    public UUID getSelectedOptionId()                { return selectedOptionId; }
    public void setSelectedOptionId(UUID v)          { this.selectedOptionId = v; }
    public String getTextValue()                     { return textValue; }
    public void setTextValue(String v)               { this.textValue = v; }
}

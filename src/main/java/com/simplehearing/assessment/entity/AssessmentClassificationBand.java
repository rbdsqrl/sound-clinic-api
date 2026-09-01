package com.simplehearing.assessment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A scoring band a fill can classify into, e.g. ISAA's "Mild Autism (70-106)" or PRBA's
 * age-gated "Adequate" bands. Age bounds are a half-open interval [min, max) — null min/max
 * means unbounded on that side. Score bounds are inclusive [min, max] on both sides.
 */
@Entity
@Table(name = "assessment_classification_bands")
public class AssessmentClassificationBand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "definition_id", nullable = false)
    private UUID definitionId;

    @Column(name = "min_age_years")
    private BigDecimal minAgeYears;

    @Column(name = "max_age_years")
    private BigDecimal maxAgeYears;

    @Column(name = "min_score")
    private Integer minScore;

    @Column(name = "max_score")
    private Integer maxScore;

    @Column(name = "label", nullable = false, length = 60)
    private String label;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public AssessmentClassificationBand() {}

    public UUID getId()                             { return id; }
    public UUID getDefinitionId()                    { return definitionId; }
    public void setDefinitionId(UUID v)              { this.definitionId = v; }
    public BigDecimal getMinAgeYears()               { return minAgeYears; }
    public void setMinAgeYears(BigDecimal v)         { this.minAgeYears = v; }
    public BigDecimal getMaxAgeYears()               { return maxAgeYears; }
    public void setMaxAgeYears(BigDecimal v)         { this.maxAgeYears = v; }
    public Integer getMinScore()                     { return minScore; }
    public void setMinScore(Integer v)               { this.minScore = v; }
    public Integer getMaxScore()                     { return maxScore; }
    public void setMaxScore(Integer v)               { this.maxScore = v; }
    public String getLabel()                         { return label; }
    public void setLabel(String v)                   { this.label = v; }
    public int getDisplayOrder()                     { return displayOrder; }
    public void setDisplayOrder(int v)               { this.displayOrder = v; }
}

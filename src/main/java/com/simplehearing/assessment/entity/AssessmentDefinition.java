package com.simplehearing.assessment.entity;

import com.simplehearing.assessment.enums.ScoringType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_definitions")
public class AssessmentDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_type", nullable = false, length = 20)
    private ScoringType scoringType;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AssessmentDefinition() {}

    public UUID getId()                             { return id; }
    public String getCode()                         { return code; }
    public void setCode(String v)                   { this.code = v; }
    public String getName()                         { return name; }
    public void setName(String v)                   { this.name = v; }
    public String getDescription()                  { return description; }
    public void setDescription(String v)             { this.description = v; }
    public ScoringType getScoringType()              { return scoringType; }
    public void setScoringType(ScoringType v)        { this.scoringType = v; }
    public int getDisplayOrder()                     { return displayOrder; }
    public void setDisplayOrder(int v)               { this.displayOrder = v; }
    public Instant getCreatedAt()                    { return createdAt; }
}

package com.simplehearing.assessment.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "assessment_categories")
public class AssessmentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "definition_id", nullable = false)
    private UUID definitionId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public AssessmentCategory() {}

    public UUID getId()                             { return id; }
    public UUID getDefinitionId()                    { return definitionId; }
    public void setDefinitionId(UUID v)              { this.definitionId = v; }
    public String getName()                         { return name; }
    public void setName(String v)                   { this.name = v; }
    public int getDisplayOrder()                     { return displayOrder; }
    public void setDisplayOrder(int v)               { this.displayOrder = v; }
}

package com.simplehearing.assessment.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "assessment_item_options")
public class AssessmentItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    /** Null for unscored items (e.g. Pre Assessment Form's intake radios). */
    @Column(name = "score")
    private Integer score;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public AssessmentItemOption() {}

    public UUID getId()                             { return id; }
    public UUID getItemId()                          { return itemId; }
    public void setItemId(UUID v)                    { this.itemId = v; }
    public String getLabel()                         { return label; }
    public void setLabel(String v)                   { this.label = v; }
    public Integer getScore()                        { return score; }
    public void setScore(Integer v)                  { this.score = v; }
    public int getDisplayOrder()                     { return displayOrder; }
    public void setDisplayOrder(int v)               { this.displayOrder = v; }
}

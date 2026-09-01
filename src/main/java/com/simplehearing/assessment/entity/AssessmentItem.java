package com.simplehearing.assessment.entity;

import com.simplehearing.assessment.enums.ItemType;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "assessment_items")
public class AssessmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "item_number", nullable = false)
    private int itemNumber;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ItemType itemType;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public AssessmentItem() {}

    public UUID getId()                             { return id; }
    public UUID getCategoryId()                      { return categoryId; }
    public void setCategoryId(UUID v)                { this.categoryId = v; }
    public int getItemNumber()                       { return itemNumber; }
    public void setItemNumber(int v)                 { this.itemNumber = v; }
    public String getText()                          { return text; }
    public void setText(String v)                    { this.text = v; }
    public ItemType getItemType()                    { return itemType; }
    public void setItemType(ItemType v)              { this.itemType = v; }
    public int getDisplayOrder()                     { return displayOrder; }
    public void setDisplayOrder(int v)               { this.displayOrder = v; }
}

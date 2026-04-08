package com.simplehearing.entity;

import com.simplehearing.enums.GalleryType;
import jakarta.persistence.*;

@Entity
@Table(name = "gallery_items")
public class GalleryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GalleryType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 1000)
    private String mediaUrl;

    @Column(length = 1000)
    private String thumbnailUrl;

    @Column(length = 10)
    private String duration;

    @Column(length = 20)
    private String colorHex;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    // Constructors
    public GalleryItem() {}

    public GalleryItem(GalleryType type, String title, String category,
                       String mediaUrl, String duration, String colorHex, int displayOrder) {
        this.type = type;
        this.title = title;
        this.category = category;
        this.mediaUrl = mediaUrl;
        this.duration = duration;
        this.colorHex = colorHex;
        this.displayOrder = displayOrder;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GalleryType getType() { return type; }
    public void setType(GalleryType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

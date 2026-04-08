package com.simplehearing.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "blog_posts")
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 1000)
    private String excerpt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private String authorName = "Dr. Suravi Dash";

    @Column(nullable = false)
    private String authorTitle = "Audiologist & Speech-Language Pathologist";

    @Column(nullable = false, length = 20)
    private String readTime;

    private boolean featured = false;

    private boolean published = true;

    @Column(nullable = false)
    private LocalDate publishedDate;

    @Column(length = 20)
    private String coverColorHex;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public BlogPost() {}

    public BlogPost(String title, String category, String excerpt, String body,
                    String readTime, boolean featured, LocalDate publishedDate, String coverColorHex) {
        this.title = title;
        this.category = category;
        this.excerpt = excerpt;
        this.body = body;
        this.readTime = readTime;
        this.featured = featured;
        this.publishedDate = publishedDate;
        this.coverColorHex = coverColorHex;
        this.published = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorTitle() { return authorTitle; }
    public void setAuthorTitle(String authorTitle) { this.authorTitle = authorTitle; }

    public String getReadTime() { return readTime; }
    public void setReadTime(String readTime) { this.readTime = readTime; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public LocalDate getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDate publishedDate) { this.publishedDate = publishedDate; }

    public String getCoverColorHex() { return coverColorHex; }
    public void setCoverColorHex(String coverColorHex) { this.coverColorHex = coverColorHex; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

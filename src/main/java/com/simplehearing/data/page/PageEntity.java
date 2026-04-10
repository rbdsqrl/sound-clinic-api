package com.simplehearing.data.page;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pages", uniqueConstraints = @UniqueConstraint(columnNames = "page_id"))
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_id", nullable = false, unique = true)
    private String pageId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("componentOrder ASC")
    private List<PageComponentEntity> sections = new ArrayList<>();

    protected PageEntity() {
        // for JPA
    }

    public PageEntity(String pageId, String title, String description) {
        this.pageId = pageId;
        this.title = title;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getPageId() {
        return pageId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<PageComponentEntity> getSections() {
        return sections;
    }

    public void addSection(PageComponentEntity section) {
        section.setPage(this);
        this.sections.add(section);
    }
}

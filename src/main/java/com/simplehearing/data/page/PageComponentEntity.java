package com.simplehearing.data.page;

import jakarta.persistence.*;

@Entity
@Table(name = "page_components")
public class PageComponentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "page_entity_id", nullable = false)
    private PageEntity page;

    @Column(name = "section_id", nullable = false)
    private String sectionId;

    @Column(nullable = false)
    private String type;

    @Column(name = "component_order", nullable = false)
    private int componentOrder;

    @Lob
    @Column(name = "data_json", nullable = false, columnDefinition = "CLOB")
    private String dataJson;

    protected PageComponentEntity() {
        // for JPA
    }

    public PageComponentEntity(String sectionId, String type, int componentOrder, String dataJson) {
        this.sectionId = sectionId;
        this.type = type;
        this.componentOrder = componentOrder;
        this.dataJson = dataJson;
    }

    public Long getId() {
        return id;
    }

    public PageEntity getPage() {
        return page;
    }

    public void setPage(PageEntity page) {
        this.page = page;
    }

    public String getSectionId() {
        return sectionId;
    }

    public String getType() {
        return type;
    }

    public int getComponentOrder() {
        return componentOrder;
    }

    public String getDataJson() {
        return dataJson;
    }
}

package com.simplehearing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hearing_services")
public class HearingService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String shortDescription;

    @Column(nullable = false, length = 2000)
    private String fullDescription;

    @ElementCollection
    @CollectionTable(name = "service_details", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "detail")
    private List<String> whatWeAddress = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceInr;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int displayOrder;

    // Constructors
    public HearingService() {}

    public HearingService(String name, String shortDescription, String fullDescription,
                          List<String> whatWeAddress, BigDecimal priceInr,
                          Integer durationMinutes, int displayOrder) {
        this.name = name;
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.whatWeAddress = whatWeAddress;
        this.priceInr = priceInr;
        this.durationMinutes = durationMinutes;
        this.displayOrder = displayOrder;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    public String getFullDescription() { return fullDescription; }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }

    public List<String> getWhatWeAddress() { return whatWeAddress; }
    public void setWhatWeAddress(List<String> whatWeAddress) { this.whatWeAddress = whatWeAddress; }

    public BigDecimal getPriceInr() { return priceInr; }
    public void setPriceInr(BigDecimal priceInr) { this.priceInr = priceInr; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}

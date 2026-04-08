package com.simplehearing.dto.response;

import com.simplehearing.entity.HearingService;

import java.math.BigDecimal;
import java.util.List;

public record HearingServiceResponse(
    Long id,
    String name,
    String shortDescription,
    String fullDescription,
    List<String> whatWeAddress,
    BigDecimal priceInr,
    Integer durationMinutes,
    int displayOrder
) {
    public static HearingServiceResponse from(HearingService s) {
        return new HearingServiceResponse(
            s.getId(), s.getName(), s.getShortDescription(), s.getFullDescription(),
            s.getWhatWeAddress(), s.getPriceInr(), s.getDurationMinutes(), s.getDisplayOrder()
        );
    }
}

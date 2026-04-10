package com.simplehearing.dto.page.section;

import java.util.List;

/** Bullet-point list of clinic differentiators. */
public record WhyChooseUsData(
    String type,
    String heading,
    String subheading,
    List<BulletPoint> points
) implements SectionData {

    /**
     * {@code iconKey} is a semantic token resolved by each client to a
     * platform icon (e.g. {@code "check_circle"}, {@code "star"}).
     */
    public record BulletPoint(String iconKey, String text) {}

    public static WhyChooseUsData of(String heading, String subheading, List<BulletPoint> points) {
        return new WhyChooseUsData("why_choose_us", heading, subheading, List.copyOf(points));
    }
}

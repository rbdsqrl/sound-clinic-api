package com.simplehearing.dto.page.section;

/** Full-width call-to-action banner, typically at the bottom of a page. */
public record CtaBannerData(
    String type,
    String heading,
    String subtext,
    String buttonLabel,
    String buttonLink
) implements SectionData {

    public static CtaBannerData of(
        String heading,
        String subtext,
        String buttonLabel,
        String buttonLink
    ) {
        return new CtaBannerData("cta_banner", heading, subtext, buttonLabel, buttonLink);
    }
}

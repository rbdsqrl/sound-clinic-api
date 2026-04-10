package com.simplehearing.dto.page.section;

import java.util.List;

/**
 * Preview grid of services shown on the home page.
 *
 * {@code services} will eventually be populated from the database.
 * For now the assembler stubs this list directly.
 *
 * {@code iconKey} is a semantic token (e.g. {@code "pediatric"}) that each
 * client maps to a platform-appropriate icon — the mobile app already does
 * this via {@code iconForService()}, the web client can map it to an SVG sprite.
 */
public record ServicesPreviewData(
    String type,
    String heading,
    String subheading,
    String ctaLabel,
    String ctaLink,
    List<ServiceCard> services
) implements SectionData {

    public record ServiceCard(long id, String name, String shortDescription, String iconKey) {}

    public static ServicesPreviewData of(
        String heading,
        String subheading,
        String ctaLabel,
        String ctaLink,
        List<ServiceCard> services
    ) {
        return new ServicesPreviewData("services_preview", heading, subheading, ctaLabel, ctaLink, List.copyOf(services));
    }
}

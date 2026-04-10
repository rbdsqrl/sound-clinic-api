package com.simplehearing.dto.page.section;

import java.util.List;

/** Hearing aid brands the clinic works with. */
public record BrandsData(
    String type,
    String heading,
    String subheading,
    List<String> names
) implements SectionData {

    public static BrandsData of(String heading, String subheading, List<String> names) {
        return new BrandsData("brands", heading, subheading, List.copyOf(names));
    }
}

package com.simplehearing.dto.page.section;

import com.simplehearing.dto.page.CtaButton;

/**
 * Hero / banner section at the top of a page.
 *
 * {@code imageUrl} is optional — null means the client uses a gradient or
 * brand colour as the background instead.
 */
public record HeroData(
    String type,
    String headline,
    String subheadline,
    String tagline,
    CtaButton primaryCta,
    CtaButton secondaryCta,
    String imageUrl
) implements SectionData {

    public static HeroData of(
        String headline,
        String subheadline,
        String tagline,
        CtaButton primaryCta,
        CtaButton secondaryCta
    ) {
        return new HeroData("hero", headline, subheadline, tagline, primaryCta, secondaryCta, null);
    }
}

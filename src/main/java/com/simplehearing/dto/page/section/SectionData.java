package com.simplehearing.dto.page.section;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed polymorphic root for section payloads.
 *
 * Jackson uses the {@code type} field that already exists on every record
 * implementation ({@code As.EXISTING_PROPERTY}) to choose the concrete subtype
 * during deserialization. The field is also written to JSON on serialisation
 * ({@code visible = true}), making the payload self-describing.
 *
 * Adding a new section type:
 *   1. Create a new record implementing this interface.
 *   2. Add it to the {@code permits} clause below.
 *   3. Register it in {@code @JsonSubTypes}.
 *   4. Wire it into the relevant {@code PageAssembler}.
 */
@JsonTypeInfo(
    use     = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible  = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = HeroData.class,            name = "hero"),
    @JsonSubTypes.Type(value = StatsBarData.class,        name = "stats_bar"),
    @JsonSubTypes.Type(value = ServicesPreviewData.class, name = "services_preview"),
    @JsonSubTypes.Type(value = BrandsData.class,          name = "brands"),
    @JsonSubTypes.Type(value = WhyChooseUsData.class,     name = "why_choose_us"),
    @JsonSubTypes.Type(value = CtaBannerData.class,       name = "cta_banner"),
})
public sealed interface SectionData
    permits HeroData, StatsBarData, ServicesPreviewData,
            BrandsData, WhyChooseUsData, CtaBannerData {}

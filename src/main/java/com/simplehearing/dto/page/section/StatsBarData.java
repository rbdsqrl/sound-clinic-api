package com.simplehearing.dto.page.section;

import java.util.List;

/** Horizontal strip of headline statistics (e.g. "13+ Years Experience"). */
public record StatsBarData(String type, List<StatItem> items) implements SectionData {

    public record StatItem(String value, String label) {}

    public static StatsBarData of(List<StatItem> items) {
        return new StatsBarData("stats_bar", List.copyOf(items));
    }
}

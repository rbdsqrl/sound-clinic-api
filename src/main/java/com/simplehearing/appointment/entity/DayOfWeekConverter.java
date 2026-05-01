package com.simplehearing.appointment.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;

/**
 * Converts {@link DayOfWeek} to and from its ISO-8601 integer value.
 * <ul>
 *   <li>MONDAY  → 1</li>
 *   <li>TUESDAY → 2</li>
 *   <li>…</li>
 *   <li>SUNDAY  → 7</li>
 * </ul>
 * This matches the {@code CHECK (day_of_week BETWEEN 1 AND 7)} constraint
 * in the {@code therapist_slots} table, and avoids the 0-based ordinal
 * that {@code @Enumerated(EnumType.ORDINAL)} would produce.
 */
@Converter
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) return null;
        return dayOfWeek.getValue(); // ISO: MONDAY=1 … SUNDAY=7
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer value) {
        if (value == null) return null;
        return DayOfWeek.of(value); // DayOfWeek.of(1) == MONDAY
    }
}

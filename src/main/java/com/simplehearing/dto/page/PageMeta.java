package com.simplehearing.dto.page;

/**
 * Page-level metadata — suitable for HTML {@code <title>} / {@code <meta>} tags
 * and app screen titles.
 */
public record PageMeta(String title, String description) {}

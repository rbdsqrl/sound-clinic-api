package com.simplehearing.dto.page;

import java.util.List;

/**
 * Top-level response envelope for every {@code GET /page/{pageId}} call.
 * Sections are ordered by {@link PageSection#order()} ascending.
 */
public record PageResponse(String pageId, PageMeta meta, List<PageSection> sections) {}

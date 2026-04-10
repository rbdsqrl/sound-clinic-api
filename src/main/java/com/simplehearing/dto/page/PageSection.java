package com.simplehearing.dto.page;

import com.simplehearing.dto.page.section.SectionData;

/**
 * A single renderable section within a page.
 *
 * <ul>
 *   <li>{@code id}    – stable identifier; clients can use this to scroll-to or deep-link</li>
 *   <li>{@code type}  – mirrors {@code data.type}; lets clients skip unknown sections gracefully
 *                       without deserialising {@code data}</li>
 *   <li>{@code order} – ascending render order; allows server-side reordering without client changes</li>
 *   <li>{@code data}  – polymorphic payload; discriminated by the {@code type} field inside the object</li>
 * </ul>
 */
public record PageSection(String id, String type, int order, SectionData data) {}

package com.simplehearing.data.page;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplehearing.dto.page.PageResponse;
import com.simplehearing.dto.page.PageSection;
import com.simplehearing.dto.page.section.SectionData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PageEntityMapper {

    private final ObjectMapper objectMapper;

    public PageEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PageResponse toPageResponse(PageEntity entity) {
        List<PageSection> sections = entity.getSections().stream()
            .map(this::toPageSection)
            .collect(Collectors.toList());

        return new PageResponse(entity.getPageId(),
            new com.simplehearing.dto.page.PageMeta(entity.getTitle(), entity.getDescription()),
            sections);
    }

    private PageSection toPageSection(PageComponentEntity component) {
        return new PageSection(
            component.getSectionId(),
            component.getType(),
            component.getComponentOrder(),
            toSectionData(component.getDataJson())
        );
    }

    private SectionData toSectionData(String json) {
        try {
            return objectMapper.readValue(json, SectionData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize section data", e);
        }
    }
}

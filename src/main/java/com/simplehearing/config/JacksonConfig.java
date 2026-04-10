package com.simplehearing.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Global Jackson configuration.
 *
 * <ul>
 *   <li><b>snake_case</b> property names — consistent with the mobile and web clients.</li>
 *   <li><b>Non-null only</b> — omits null fields from the JSON response to keep payloads lean.</li>
 *   <li><b>No timestamp dates</b> — dates/times serialize as ISO-8601 strings.</li>
 * </ul>
 *
 * The {@link com.simplehearing.dto.page.section.SectionData} sealed interface uses
 * {@code @JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")}
 * so each section's {@code type} field doubles as the Jackson discriminator.
 * No additional configuration is required here for polymorphic serialization.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}

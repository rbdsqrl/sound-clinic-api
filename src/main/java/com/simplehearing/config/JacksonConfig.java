package com.simplehearing.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Global Jackson configuration.
 *
 * <ul>
 *   <li><b>camelCase</b> property names (Jackson default) — matches web and mobile clients.</li>
 *   <li><b>Non-null only</b> — omits null fields from JSON responses to keep payloads lean.</li>
 *   <li><b>No timestamp dates</b> — dates/times serialize as ISO-8601 strings.</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}

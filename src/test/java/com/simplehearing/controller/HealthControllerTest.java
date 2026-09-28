package com.simplehearing.controller;

import com.simplehearing.auth.security.TokenService;
import com.simplehearing.config.SecurityConfig;
import com.simplehearing.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvcTest slice for HealthController.
 *
 * JwtAuthFilter is a real bean (it's a Filter component that WebMvcTest picks up),
 * so its dependencies are mocked here. Without a Bearer token in the request, the
 * real filter short-circuits and calls chain.doFilter() — the health endpoints are
 * also declared as PUBLIC_PATHS in SecurityConfig, so they pass through unauthenticated.
 */
@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
@org.springframework.test.context.TestPropertySource(properties = {
    "app.cors.allowed-origins=http://localhost:3000"
})
class HealthControllerTest {

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRootEndpoint_ReturnsServiceInfo() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service", is("Simple Hearing & Speech Care API")))
            .andExpect(jsonPath("$.status", is("running")))
            .andExpect(jsonPath("$", hasKey("timestamp")));
    }

    @Test
    void testRootEndpoint_ResponseContainsAllRequiredFields() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").exists())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testHealthEndpoint_ReturnsUpStatus() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")))
            .andExpect(jsonPath("$.service", is("simple-hearing-api")))
            .andExpect(jsonPath("$", hasKey("timestamp")));
    }

    @Test
    void testHealthEndpoint_ResponseContainsAllRequiredFields() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.service").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testRootEndpoint_ReturnsOkHttpStatus() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk());
    }

    @Test
    void testHealthEndpoint_ReturnsOkHttpStatus() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk());
    }

    @Test
    void testHealthDbEndpoint_ReturnsUpStatusWhenQuerySucceeds() throws Exception {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        mockMvc.perform(get("/health/db"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")))
            .andExpect(jsonPath("$.component", is("database")))
            .andExpect(jsonPath("$", hasKey("timestamp")));
    }

    @Test
    void testHealthDbEndpoint_ReturnsDownStatusWhenQueryFails() throws Exception {
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class)))
            .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("connection refused"));

        mockMvc.perform(get("/health/db"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status", is("DOWN")))
            .andExpect(jsonPath("$.component", is("database")));
    }
}

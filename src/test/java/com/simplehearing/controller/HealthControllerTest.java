package com.simplehearing.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

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
}

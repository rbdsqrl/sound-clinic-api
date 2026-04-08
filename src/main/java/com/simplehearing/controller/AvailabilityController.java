package com.simplehearing.controller;

import com.simplehearing.dto.response.AvailabilityResponse;
import com.simplehearing.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/availability")
@Tag(name = "Availability", description = "Available appointment slots")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    @Operation(summary = "Get availability for a date range")
    public ResponseEntity<List<AvailabilityResponse>> getRange(
        @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().toString()}")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(availabilityService.getAvailabilityRange(from, days));
    }

    @GetMapping("/{date}")
    @Operation(summary = "Get available slots for a specific date")
    public ResponseEntity<AvailabilityResponse> getForDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(availabilityService.getForDate(date));
    }
}

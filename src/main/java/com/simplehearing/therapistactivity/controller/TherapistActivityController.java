package com.simplehearing.therapistactivity.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.therapistactivity.dto.TherapistActivityResponse;
import com.simplehearing.therapistactivity.service.TherapistActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Therapist Activity", description = "Management's daily documentation check — did a therapist keep session notes and share media today, per child")
@RestController
@RequestMapping("/api/v1/therapist-activity")
public class TherapistActivityController {

    private final TherapistActivityService service;

    public TherapistActivityController(TherapistActivityService service) {
        this.service = service;
    }

    @Operation(summary = "One therapist's session notes, free-text notes and media for one day, broken down per child")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<TherapistActivityResponse>> getActivity(
            @RequestParam UUID therapistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getActivity(principal.getOrgId(), therapistId, date)));
    }
}

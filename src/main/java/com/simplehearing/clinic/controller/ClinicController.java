package com.simplehearing.clinic.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.clinic.dto.ClinicResponse;
import com.simplehearing.clinic.dto.CreateClinicRequest;
import com.simplehearing.clinic.service.ClinicService;
import com.simplehearing.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Clinics", description = "Clinic management within an organisation")
@RestController
@RequestMapping("/api/v1/clinics")
public class ClinicController {

    private final ClinicService clinicService;

    public ClinicController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    @Operation(summary = "Create a new clinic")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ClinicResponse>> create(
            @Valid @RequestBody CreateClinicRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(clinicService.create(request, principal)));
    }

    @Operation(summary = "List all clinics in your organisation")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ClinicResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(clinicService.listForOrg(principal)));
    }

    @Operation(summary = "Get a clinic by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ClinicResponse>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(clinicService.get(id, principal)));
    }

    @Operation(summary = "Update a clinic")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ClinicResponse>> update(
            @PathVariable UUID id,
            @RequestBody CreateClinicRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(clinicService.update(id, request, principal)));
    }
}

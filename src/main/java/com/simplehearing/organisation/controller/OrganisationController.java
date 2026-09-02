package com.simplehearing.organisation.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.organisation.dto.OrganisationResponse;
import com.simplehearing.organisation.dto.UpdateOrganisationRequest;
import com.simplehearing.organisation.service.OrganisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.Set;

@Tag(name = "Organisation", description = "Organisation profile management")
@RestController
@RequestMapping("/api/v1/organisation")
public class OrganisationController {

    private final OrganisationService organisationService;

    public OrganisationController(OrganisationService organisationService) {
        this.organisationService = organisationService;
    }

    @Operation(summary = "Get your organisation")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<OrganisationResponse>> getMyOrg(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(organisationService.getMyOrg(principal)));
    }

    @Operation(summary = "Update your organisation")
    @PatchMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<OrganisationResponse>> updateMyOrg(
            @RequestBody UpdateOrganisationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(organisationService.updateMyOrg(request, principal)));
    }

    @Operation(
        summary = "Get just the weekly off days",
        description = "A narrower read than GET /organisation, for roles that can schedule sessions "
                    + "but not see the full org profile — lets the enrollment schedule form default "
                    + "Session Days away from days that would never generate a session anyway."
    )
    @GetMapping("/weekly-off-days")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<Set<DayOfWeek>>> getWeeklyOffDays(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(organisationService.getWeeklyOffDays(principal)));
    }
}

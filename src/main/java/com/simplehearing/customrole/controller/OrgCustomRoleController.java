package com.simplehearing.customrole.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.customrole.service.OrgCustomRoleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organisation/custom-roles")
public class OrgCustomRoleController {

    public record CustomRoleResponse(UUID id, String name) {}
    public record CreateCustomRoleRequest(@NotBlank String name) {}

    private final OrgCustomRoleService service;

    public OrgCustomRoleController(OrgCustomRoleService service) {
        this.service = service;
    }

    @Operation(summary = "The organisation's Custom Role names, for the member profile picker")
    @GetMapping
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<CustomRoleResponse>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(service.list(principal.getOrgId()).stream()
                .map(r -> new CustomRoleResponse(r.getId(), r.getName())).toList()));
    }

    @Operation(summary = "Add a Custom Role (returns the existing one if the name is already there)")
    @PostMapping
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<CustomRoleResponse>> create(
            @Valid @RequestBody CreateCustomRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var r = service.findOrCreate(principal.getOrgId(), request.name(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new CustomRoleResponse(r.getId(), r.getName())));
    }

    @Operation(summary = "Remove a Custom Role from the picker — members who already have it keep it")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        service.remove(principal.getOrgId(), id);
        return ResponseEntity.noContent().build();
    }
}

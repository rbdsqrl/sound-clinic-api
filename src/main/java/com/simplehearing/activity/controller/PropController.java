package com.simplehearing.activity.controller;

import com.simplehearing.activity.dto.PropResponse;
import com.simplehearing.activity.entity.Prop;
import com.simplehearing.activity.repository.PropRepository;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Activity Props", description = "Per-org props/materials used on activities")
@RestController
@RequestMapping("/api/v1/activities/props")
public class PropController {

    private final PropRepository propRepository;

    public PropController(PropRepository propRepository) {
        this.propRepository = propRepository;
    }

    @Operation(summary = "List all active props for the org")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<PropResponse>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        List<PropResponse> results = propRepository
                .findByOrgIdAndIsActiveTrueOrderByNameAsc(principal.getOrgId())
                .stream().map(PropResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @Operation(summary = "Add a prop for this org — also used to add one inline while authoring an activity")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<PropResponse>> create(
            @RequestBody CreatePropRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String name = request.name().trim();

        // A free-typed "add new" control invites near-duplicates — reuse an existing match
        // rather than creating a second "Flashcards" alongside "flashcards".
        Prop existing = propRepository.findByOrgIdAndNameIgnoreCase(principal.getOrgId(), name).orElse(null);
        if (existing != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(PropResponse.from(existing)));
        }

        Prop prop = new Prop();
        prop.setOrgId(principal.getOrgId());
        prop.setName(name);
        Prop saved = propRepository.save(prop);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(PropResponse.from(saved)));
    }

    @Operation(summary = "Delete a prop")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Prop prop = propRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prop not found"));
        if (!prop.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        propRepository.delete(prop);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    public record CreatePropRequest(@NotBlank String name) {}
}

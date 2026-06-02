package com.simplehearing.therapy.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.therapy.dto.TherapyResponse;
import com.simplehearing.therapy.entity.Therapy;
import com.simplehearing.therapy.repository.TherapyRepository;
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

@Tag(name = "Therapies", description = "Per-org therapy types")
@RestController
@RequestMapping("/api/v1/therapies")
public class TherapyController {

    private final TherapyRepository therapyRepository;

    public TherapyController(TherapyRepository therapyRepository) {
        this.therapyRepository = therapyRepository;
    }

    @Operation(summary = "List all active therapies for the org")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TherapyResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<TherapyResponse> results = therapyRepository
                .findByOrgIdAndIsActiveTrueOrderByNameAsc(principal.getOrgId())
                .stream().map(TherapyResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @Operation(summary = "Add a therapy type for this org")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TherapyResponse>> create(
            @RequestBody CreateTherapyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Therapy therapy = new Therapy();
        therapy.setOrgId(principal.getOrgId());
        therapy.setName(request.name().trim());
        Therapy saved = therapyRepository.save(therapy);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(TherapyResponse.from(saved)));
    }

    @Operation(summary = "Delete a therapy type")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Therapy therapy = therapyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapy not found"));
        if (!therapy.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        therapyRepository.delete(therapy);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    public record CreateTherapyRequest(@NotBlank String name) {}
}

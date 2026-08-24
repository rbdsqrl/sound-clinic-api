package com.simplehearing.activity.controller;

import com.simplehearing.activity.dto.LanguageResponse;
import com.simplehearing.activity.entity.Language;
import com.simplehearing.activity.repository.LanguageRepository;
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

@Tag(name = "Activity Languages", description = "Per-org languages used on activities")
@RestController
@RequestMapping("/api/v1/activities/languages")
public class LanguageController {

    private final LanguageRepository languageRepository;

    public LanguageController(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    @Operation(summary = "List all active languages for the org")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST')")
    public ResponseEntity<ApiResponse<List<LanguageResponse>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        List<LanguageResponse> results = languageRepository
                .findByOrgIdAndIsActiveTrueOrderByNameAsc(principal.getOrgId())
                .stream().map(LanguageResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @Operation(summary = "Add a language for this org")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LanguageResponse>> create(
            @RequestBody CreateLanguageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Language language = new Language();
        language.setOrgId(principal.getOrgId());
        language.setName(request.name().trim());
        Language saved = languageRepository.save(language);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(LanguageResponse.from(saved)));
    }

    @Operation(summary = "Delete a language")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Language language = languageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Language not found"));
        if (!language.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        languageRepository.delete(language);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    public record CreateLanguageRequest(@NotBlank String name) {}
}

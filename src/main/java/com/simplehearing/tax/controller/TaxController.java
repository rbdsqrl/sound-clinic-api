package com.simplehearing.tax.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.tax.dto.TaxResponse;
import com.simplehearing.tax.entity.Tax;
import com.simplehearing.tax.repository.TaxRepository;
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

@Tag(name = "Taxes", description = "Per-org tax rate management")
@RestController
@RequestMapping("/api/v1/taxes")
public class TaxController {

    private final TaxRepository taxRepository;

    public TaxController(TaxRepository taxRepository) {
        this.taxRepository = taxRepository;
    }

    @Operation(summary = "List all active tax rates for the org")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TaxResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<TaxResponse> results = taxRepository
                .findByOrgIdAndIsActiveTrueOrderByNameAsc(principal.getOrgId())
                .stream().map(TaxResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @Operation(summary = "Add a tax rate for this org")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaxResponse>> create(
            @RequestBody CreateTaxRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Tax tax = new Tax();
        tax.setOrgId(principal.getOrgId());
        tax.setName(request.name().trim());
        tax.setRate(request.rate());
        Tax saved = taxRepository.save(tax);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(TaxResponse.from(saved)));
    }

    @Operation(summary = "Delete a tax rate")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Tax tax = taxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax not found"));
        if (!tax.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        taxRepository.delete(tax);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    public record CreateTaxRequest(@NotBlank String name, double rate) {}
}

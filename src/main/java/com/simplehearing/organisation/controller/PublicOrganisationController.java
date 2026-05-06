package com.simplehearing.organisation.controller;

import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.organisation.repository.OrganisationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Public", description = "Unauthenticated endpoints for the public website")
@RestController
@RequestMapping("/api/v1/public")
public class PublicOrganisationController {

    public record PublicOrgInfo(UUID id, String name) {}

    private final OrganisationRepository organisationRepository;

    public PublicOrganisationController(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }

    @Operation(summary = "Get basic organisation info for the public website")
    @GetMapping("/organisation")
    public ResponseEntity<ApiResponse<PublicOrgInfo>> getPublicOrgInfo() {
        return organisationRepository.findAll().stream()
                .findFirst()
                .map(org -> ResponseEntity.ok(
                        ApiResponse.success(new PublicOrgInfo(org.getId(), org.getName()))))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }
}

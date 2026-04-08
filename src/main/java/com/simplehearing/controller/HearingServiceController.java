package com.simplehearing.controller;

import com.simplehearing.dto.response.HearingServiceResponse;
import com.simplehearing.entity.HearingService;
import com.simplehearing.service.HearingServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Services", description = "Hearing & speech care services")
public class HearingServiceController {

    private final HearingServiceService serviceService;

    public HearingServiceController(HearingServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @GetMapping
    @Operation(summary = "List all active services")
    public ResponseEntity<List<HearingServiceResponse>> getAll() {
        return ResponseEntity.ok(serviceService.getAllActive());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a service by ID")
    public ResponseEntity<HearingServiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new service (admin)")
    public ResponseEntity<HearingServiceResponse> create(@RequestBody HearingService service) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceService.create(service));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a service (admin)")
    public ResponseEntity<HearingServiceResponse> update(
        @PathVariable Long id, @RequestBody HearingService service) {
        return ResponseEntity.ok(serviceService.update(id, service));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a service (admin)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

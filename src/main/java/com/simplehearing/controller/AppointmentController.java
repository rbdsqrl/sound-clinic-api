package com.simplehearing.controller;

import com.simplehearing.dto.request.AppointmentRequest;
import com.simplehearing.dto.response.AppointmentResponse;
import com.simplehearing.enums.AppointmentStatus;
import com.simplehearing.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Appointment booking and management")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @Operation(summary = "Create appointment and Razorpay order")
    public ResponseEntity<AppointmentResponse> create(
        @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(appointmentService.createAppointment(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all appointments (admin)")
    public ResponseEntity<Page<AppointmentResponse>> getAll(
        @RequestParam(required = false) AppointmentStatus status,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(appointmentService.getByStatus(status, pageable));
        }
        return ResponseEntity.ok(appointmentService.getAllAppointments(pageable));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update appointment status (admin)")
    public ResponseEntity<AppointmentResponse> updateStatus(
        @PathVariable Long id,
        @RequestBody Map<String, String> body) {
        AppointmentStatus newStatus = AppointmentStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(appointmentService.updateStatus(id, newStatus));
    }
}

package com.simplehearing.appointment.controller;

import com.simplehearing.appointment.dto.*;
import com.simplehearing.appointment.service.AppointmentService;
import com.simplehearing.auth.security.UserPrincipal;
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

@Tag(name = "Appointments", description = "Therapist availability slots and appointment booking")
@RestController
@RequestMapping("/api/v1")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // ── Therapist Slots ───────────────────────────────────────────────────────

    @Operation(summary = "Create a recurring availability slot for a therapist")
    @PostMapping("/availability-slots")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SlotResponse>> createSlot(
            @Valid @RequestBody CreateSlotRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(appointmentService.createSlot(request, principal)));
    }

    @Operation(summary = "List availability slots — optionally filter by therapistId")
    @GetMapping("/availability-slots")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'THERAPIST', 'DOCTOR', 'PARENT')")
    public ResponseEntity<ApiResponse<List<SlotResponse>>> listSlots(
            @RequestParam(required = false) UUID therapistId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.listSlots(therapistId, principal)));
    }

    @Operation(summary = "Delete a therapist availability slot")
    @DeleteMapping("/availability-slots/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        appointmentService.deleteSlot(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ── Appointments ──────────────────────────────────────────────────────────

    @Operation(summary = "Book an appointment (parent)")
    @PostMapping("/appointments")
    @PreAuthorize("hasAnyRole('PARENT', 'BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> book(
            @Valid @RequestBody BookAppointmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(appointmentService.book(request, principal)));
    }

    @Operation(summary = "List appointments (role-scoped)")
    @GetMapping("/appointments")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'THERAPIST', 'DOCTOR', 'PARENT')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.listForCaller(principal)));
    }

    @Operation(summary = "Update appointment status")
    @PatchMapping("/appointments/{id}/status")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'THERAPIST', 'DOCTOR', 'PARENT')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.updateStatus(id, request, principal)));
    }
}

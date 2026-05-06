package com.simplehearing.subscription.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.enums.PatientStage;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.subscription.dto.CreateSubscriptionRequest;
import com.simplehearing.subscription.dto.SubscriptionResponse;
import com.simplehearing.subscription.dto.UpdatePaymentRequest;
import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.enums.SubscriptionPaymentStatus;
import com.simplehearing.subscription.enums.SubscriptionStatus;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Tag(name = "Subscriptions", description = "Patient therapy subscriptions and payment tracking")
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;
    private final PatientRepository patientRepository;

    public SubscriptionController(
            SubscriptionRepository subscriptionRepository,
            ProgramRepository programRepository,
            PatientRepository patientRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
        this.patientRepository = patientRepository;
    }

    // ── List subscriptions for a patient ──────────────────────────────────────

    @Operation(summary = "List subscriptions for a patient")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> list(
            @RequestParam UUID patientId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Subscription> subs = subscriptionRepository
                .findByOrgIdAndPatientIdOrderByCreatedAtDesc(principal.getOrgId(), patientId);

        List<SubscriptionResponse> result = subs.stream().map(sub -> {
            Program program = programRepository.findById(sub.getProgramId()).orElse(null);
            String name = program != null ? program.getName() : "Unknown Program";
            return SubscriptionResponse.from(sub, name);
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Create a subscription ─────────────────────────────────────────────────

    @Operation(summary = "Allocate a subscription to a patient")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> create(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));

        if (!program.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (!program.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot subscribe to an inactive program");
        }

        Subscription sub = new Subscription();
        sub.setOrgId(principal.getOrgId());
        sub.setPatientId(request.patientId());
        sub.setProgramId(request.programId());
        sub.setNumSessions(request.numSessions());
        sub.setPerSessionCost(program.getPerSessionCost());   // snapshot at creation time
        sub.setNotes(request.notes());
        sub.setCreatedBy(principal.getId());

        Subscription saved = subscriptionRepository.save(sub);

        // Auto-advance patient stage to ENROLLMENT if not yet there
        patientRepository.findById(request.patientId()).ifPresent(patient -> {
            PatientStage stage = patient.getStage();
            if (stage == PatientStage.INQUIRY_CONVERTED
                    || stage == PatientStage.PRE_ASSESSMENT
                    || stage == PatientStage.ASSESSMENT_DONE) {
                patient.setStage(PatientStage.ENROLLMENT);
                patientRepository.save(patient);
            }
        });

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(SubscriptionResponse.from(saved, program.getName())));
    }

    // ── Record payment (discount + amount paid) ───────────────────────────────

    @Operation(summary = "Record payment and discount for a subscription")
    @PatchMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'ADMIN', 'BUSINESS_OWNER')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        if (!sub.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot update payment on a cancelled subscription");
        }

        sub.setDiscountPercent(request.discountPercent());
        sub.setAmountPaid(request.amountPaid());
        if (request.paymentNotes() != null) {
            sub.setPaymentNotes(request.paymentNotes());
        }

        // Derive payment status from amount paid vs total due
        BigDecimal discount = request.discountPercent()
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalDue = sub.getPerSessionCost()
                .multiply(BigDecimal.valueOf(sub.getNumSessions()))
                .multiply(BigDecimal.ONE.subtract(discount))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal paid = request.amountPaid().setScale(2, RoundingMode.HALF_UP);

        if (paid.compareTo(BigDecimal.ZERO) == 0) {
            sub.setPaymentStatus(SubscriptionPaymentStatus.PENDING);
        } else if (paid.compareTo(totalDue) >= 0) {
            sub.setPaymentStatus(SubscriptionPaymentStatus.PAID);
        } else {
            sub.setPaymentStatus(SubscriptionPaymentStatus.PARTIAL);
        }

        Subscription saved = subscriptionRepository.save(sub);
        Program program = programRepository.findById(saved.getProgramId()).orElse(null);
        String name = program != null ? program.getName() : "Unknown Program";

        return ResponseEntity.ok(ApiResponse.success(SubscriptionResponse.from(saved, name)));
    }

    // ── Cancel a subscription ─────────────────────────────────────────────────

    @Operation(summary = "Cancel a subscription")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        if (!sub.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Subscription is already cancelled");
        }

        sub.setStatus(SubscriptionStatus.CANCELLED);
        Subscription saved = subscriptionRepository.save(sub);

        Program program = programRepository.findById(saved.getProgramId()).orElse(null);
        String name = program != null ? program.getName() : "Unknown Program";

        return ResponseEntity.ok(ApiResponse.success(SubscriptionResponse.from(saved, name)));
    }
}

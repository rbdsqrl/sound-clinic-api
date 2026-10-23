package com.simplehearing.subscription.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.enums.PatientStage;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.program.dto.ProgramResponse;
import com.simplehearing.program.entity.Program;
import com.simplehearing.program.repository.ProgramRepository;
import com.simplehearing.subscription.dto.AddPaymentRequest;
import com.simplehearing.subscription.dto.CreateSubscriptionRequest;
import com.simplehearing.subscription.dto.SubscriptionPaymentResponse;
import com.simplehearing.subscription.dto.SubscriptionResponse;
import com.simplehearing.subscription.dto.UpdatePaymentRequest;
import com.simplehearing.subscription.entity.Subscription;
import com.simplehearing.subscription.enums.SubscriptionPaymentStatus;
import com.simplehearing.subscription.enums.SubscriptionStatus;
import com.simplehearing.subscription.repository.SubscriptionPaymentRepository;
import com.simplehearing.subscription.repository.SubscriptionRepository;
import com.simplehearing.subscription.service.EnrollmentCancellationService;
import com.simplehearing.subscription.service.EnrollmentPaymentActivationService;
import com.simplehearing.subscription.service.SubscriptionPaymentService;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import com.simplehearing.tax.entity.Tax;
import com.simplehearing.tax.repository.TaxRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Subscriptions", description = "Patient therapy subscriptions and payment tracking")
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final ProgramRepository programRepository;
    private final PatientRepository patientRepository;
    private final TaxRepository taxRepository;
    private final EnrollmentPaymentActivationService enrollmentPaymentActivationService;
    private final EnrollmentCancellationService enrollmentCancellationService;
    private final SubscriptionPaymentService paymentService;
    private final SubscriptionPaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public SubscriptionController(
            SubscriptionRepository subscriptionRepository,
            ProgramRepository programRepository,
            PatientRepository patientRepository,
            TaxRepository taxRepository,
            EnrollmentPaymentActivationService enrollmentPaymentActivationService,
            EnrollmentCancellationService enrollmentCancellationService,
            SubscriptionPaymentService paymentService,
            SubscriptionPaymentRepository paymentRepository,
            UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.programRepository = programRepository;
        this.patientRepository = patientRepository;
        this.taxRepository = taxRepository;
        this.enrollmentPaymentActivationService = enrollmentPaymentActivationService;
        this.enrollmentCancellationService = enrollmentCancellationService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    // ── List subscriptions for a patient ──────────────────────────────────────

    @Operation(summary = "List subscriptions for a patient")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'PARENT', 'OFFICE_ADMIN')")
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
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
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

        Tax tax = program.getTaxId() != null ? taxRepository.findById(program.getTaxId()).orElse(null) : null;

        Subscription sub = new Subscription();
        sub.setOrgId(principal.getOrgId());
        sub.setPatientId(request.patientId());
        sub.setProgramId(request.programId());
        sub.setNumSessions(request.numSessions());
        // A caller-supplied fee overrides the program's price for this case only — the
        // program's own price is never touched. Omit it to keep the old snapshot-at-creation
        // behaviour (program price, tax included).
        sub.setPerSessionCost(request.perSessionCost() != null
                ? request.perSessionCost()
                : ProgramResponse.effectiveCost(program, tax));
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
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'PARENT', 'OFFICE_ADMIN')")
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

        BigDecimal previousPaid = sub.getAmountPaid();

        // An agreed total stands until someone changes the discount here — then the percentage takes over again.
        if (sub.getTotalOverride() != null && request.discountPercent().compareTo(sub.getDiscountPercent()) != 0) {
            sub.setTotalOverride(null);
        }
        sub.setDiscountPercent(request.discountPercent());
        sub.setAmountPaid(request.amountPaid());
        if (request.paymentNotes() != null) {
            sub.setPaymentNotes(request.paymentNotes());
        }

        // Status is derived from amount paid vs total due (see SubscriptionPaymentService), and the
        // moment it reaches PAID, sessions held awaiting payment are released — once per genuine transition.
        Subscription saved = paymentService.refreshStatus(sub);

        // This screen sets the running total outright, so keep the payment history in step by logging
        // the difference (a reduction shows as a negative correction).
        BigDecimal delta = request.amountPaid().subtract(previousPaid);
        if (delta.signum() != 0) {
            paymentService.logEntry(saved, delta, LocalDate.now(), null, request.paymentNotes(), principal.getId());
        }

        Program program = programRepository.findById(saved.getProgramId()).orElse(null);
        String name = program != null ? program.getName() : "Unknown Program";

        return ResponseEntity.ok(ApiResponse.success(SubscriptionResponse.from(saved, name)));
    }

    // ── Payment history ───────────────────────────────────────────────────────

    @Operation(summary = "A plan's payments, newest first")
    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<SubscriptionPaymentResponse>>> listPayments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Subscription sub = requireOwnSubscription(id, principal);
        var payments = paymentRepository.findBySubscriptionIdOrderByPaidOnDescCreatedAtDesc(sub.getId());
        Map<UUID, User> users = userRepository.findAllById(
                        payments.stream().map(p -> p.getRecordedBy()).filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return ResponseEntity.ok(ApiResponse.success(payments.stream().map(p -> {
            User u = p.getRecordedBy() != null ? users.get(p.getRecordedBy()) : null;
            return SubscriptionPaymentResponse.from(p, u != null ? u.getFirstName() + " " + u.getLastName() : null);
        }).toList()));
    }

    @Operation(summary = "Register one more payment against a plan — added to what has already been paid")
    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('CLINIC_HEAD', 'BUSINESS_OWNER', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> addPayment(
            @PathVariable UUID id,
            @Valid @RequestBody AddPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Subscription sub = requireOwnSubscription(id, principal);
        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot add a payment to a cancelled subscription");
        }
        Subscription saved = paymentService.addPayment(
                sub, request.amount(), request.paidOn(), request.mode(), request.note(), principal.getId());

        Program program = programRepository.findById(saved.getProgramId()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                SubscriptionResponse.from(saved, program != null ? program.getName() : "Unknown Program")));
    }

    private Subscription requireOwnSubscription(UUID id, UserPrincipal principal) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        if (!sub.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return sub;
    }

    // ── Cancel a subscription ─────────────────────────────────────────────────

    @Operation(summary = "Cancel a subscription")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'OFFICE_ADMIN')")
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

        // Cancelling the plan doesn't by itself touch what it already generated — without this,
        // the enrollment stayed ACTIVE and every still-ahead session stayed SCHEDULED, fully
        // live on the calendar and actionable, with no indication the plan behind them was gone.
        enrollmentCancellationService.cancelForSubscription(saved.getId());

        Program program = programRepository.findById(saved.getProgramId()).orElse(null);
        String name = program != null ? program.getName() : "Unknown Program";

        return ResponseEntity.ok(ApiResponse.success(SubscriptionResponse.from(saved, name)));
    }
}

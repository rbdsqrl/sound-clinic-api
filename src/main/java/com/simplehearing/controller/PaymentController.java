package com.simplehearing.controller;

import com.simplehearing.dto.request.PaymentFailureRequest;
import com.simplehearing.dto.request.PaymentVerifyRequest;
import com.simplehearing.dto.response.PaymentResponse;
import com.simplehearing.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing and verification")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay signature and confirm appointment")
    public ResponseEntity<PaymentResponse> verify(
        @Valid @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(paymentService.verifyAndConfirm(request));
    }

    @PostMapping("/failure")
    @Operation(summary = "Record a failed payment attempt")
    public ResponseEntity<PaymentResponse> failure(
        @Valid @RequestBody PaymentFailureRequest request) {
        return ResponseEntity.ok(paymentService.recordFailure(request));
    }

    @GetMapping("/{appointmentId}")
    @Operation(summary = "Get payment status by appointment ID")
    public ResponseEntity<PaymentResponse> getByAppointment(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(paymentService.getByAppointmentId(appointmentId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all payments (admin)")
    public ResponseEntity<Page<PaymentResponse>> getAll(
        @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAllPayments(pageable));
    }
}

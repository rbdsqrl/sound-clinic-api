package com.simplehearing.service;

import com.simplehearing.dto.request.PaymentFailureRequest;
import com.simplehearing.dto.request.PaymentVerifyRequest;
import com.simplehearing.dto.response.PaymentResponse;
import com.simplehearing.entity.Appointment;
import com.simplehearing.entity.Payment;
import com.simplehearing.enums.AppointmentStatus;
import com.simplehearing.enums.PaymentStatus;
import com.simplehearing.exception.PaymentVerificationException;
import com.simplehearing.exception.ResourceNotFoundException;
import com.simplehearing.repository.AppointmentRepository;
import com.simplehearing.repository.PaymentRepository;
import com.simplehearing.util.RazorpaySignatureUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final AppointmentRepository appointmentRepo;
    private final RazorpaySignatureUtil signatureUtil;

    @Value("${app.razorpay.key-secret}")
    private String razorpaySecret;

    public PaymentService(PaymentRepository paymentRepo,
                          AppointmentRepository appointmentRepo,
                          RazorpaySignatureUtil signatureUtil) {
        this.paymentRepo = paymentRepo;
        this.appointmentRepo = appointmentRepo;
        this.signatureUtil = signatureUtil;
    }

    @Transactional
    public PaymentResponse verifyAndConfirm(PaymentVerifyRequest request) {
        Appointment appointment = appointmentRepo.findById(request.appointmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", request.appointmentId()));

        Payment payment = paymentRepo.findByAppointmentId(request.appointmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Payment for appointment", request.appointmentId()));

        boolean valid = signatureUtil.verify(
            request.razorpayOrderId(),
            request.razorpayPaymentId(),
            request.razorpaySignature(),
            razorpaySecret
        );

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepo.save(payment);
            throw new PaymentVerificationException("Payment signature verification failed.");
        }

        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setRazorpaySignature(request.razorpaySignature());
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepo.save(payment);

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepo.save(appointment);

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse recordFailure(PaymentFailureRequest request) {
        Payment payment = paymentRepo.findByRazorpayOrderId(request.razorpayOrderId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Payment with order ID: " + request.razorpayOrderId()));
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepo.save(payment);
        return PaymentResponse.from(payment);
    }

    public PaymentResponse getByAppointmentId(Long appointmentId) {
        Payment payment = paymentRepo.findByAppointmentId(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment for appointment", appointmentId));
        return PaymentResponse.from(payment);
    }

    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepo.findAllByOrderByCreatedAtDesc(pageable)
            .map(PaymentResponse::from);
    }
}

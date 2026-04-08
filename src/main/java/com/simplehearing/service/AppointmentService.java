package com.simplehearing.service;

import com.simplehearing.dto.request.AppointmentRequest;
import com.simplehearing.dto.response.AppointmentResponse;
import com.simplehearing.entity.Appointment;
import com.simplehearing.entity.HearingService;
import com.simplehearing.entity.Payment;
import com.simplehearing.enums.AppointmentStatus;
import com.simplehearing.enums.PaymentStatus;
import com.simplehearing.exception.ResourceNotFoundException;
import com.simplehearing.exception.SlotUnavailableException;
import com.simplehearing.repository.AppointmentRepository;
import com.simplehearing.repository.HearingServiceRepository;
import com.simplehearing.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final HearingServiceRepository serviceRepo;
    private final PaymentRepository paymentRepo;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    public AppointmentService(AppointmentRepository appointmentRepo,
                              HearingServiceRepository serviceRepo,
                              PaymentRepository paymentRepo) {
        this.appointmentRepo = appointmentRepo;
        this.serviceRepo = serviceRepo;
        this.paymentRepo = paymentRepo;
    }

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        // Verify slot is available
        boolean slotTaken = appointmentRepo.existsByAppointmentDateAndTimeSlotAndStatusNot(
            request.appointmentDate(), request.timeSlot(), AppointmentStatus.CANCELLED);
        if (slotTaken) {
            throw new SlotUnavailableException(
                request.appointmentDate().toString(), request.timeSlot());
        }

        HearingService service = serviceRepo.findById(request.serviceId())
            .orElseThrow(() -> new ResourceNotFoundException("Service", request.serviceId()));

        Appointment appointment = new Appointment();
        appointment.setService(service);
        appointment.setAppointmentDate(request.appointmentDate());
        appointment.setTimeSlot(request.timeSlot());
        appointment.setPatientName(request.patientName());
        appointment.setPatientPhone(request.patientPhone());
        appointment.setNotes(request.notes());
        appointment.setStatus(AppointmentStatus.PENDING);

        // Simulate Razorpay order ID (replace with SDK call in production)
        String simulatedOrderId = "order_sim_" + System.currentTimeMillis();
        appointment.setRazorpayOrderId(simulatedOrderId);

        try {
            appointment = appointmentRepo.save(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new SlotUnavailableException(
                request.appointmentDate().toString(), request.timeSlot());
        }

        // Create pending payment record
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setRazorpayOrderId(simulatedOrderId);
        payment.setAmount(service.getPriceInr());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepo.save(payment);

        return AppointmentResponse.from(appointment, razorpayKeyId);
    }

    public AppointmentResponse getById(Long id) {
        Appointment appointment = appointmentRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        return AppointmentResponse.from(appointment, razorpayKeyId);
    }

    public Page<AppointmentResponse> getAllAppointments(Pageable pageable) {
        return appointmentRepo.findAllByOrderByCreatedAtDesc(pageable)
            .map(a -> AppointmentResponse.from(a, razorpayKeyId));
    }

    public Page<AppointmentResponse> getByStatus(AppointmentStatus status, Pageable pageable) {
        return appointmentRepo.findByStatusOrderByCreatedAtDesc(status, pageable)
            .map(a -> AppointmentResponse.from(a, razorpayKeyId));
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        appointment.setStatus(newStatus);
        return AppointmentResponse.from(appointmentRepo.save(appointment), razorpayKeyId);
    }
}

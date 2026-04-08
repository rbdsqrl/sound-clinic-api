package com.simplehearing.repository;

import com.simplehearing.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByAppointmentId(Long appointmentId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

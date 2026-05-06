package com.simplehearing.inquiry.repository;

import com.simplehearing.inquiry.entity.Inquiry;
import com.simplehearing.inquiry.enums.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {
    List<Inquiry> findByOrgIdOrderByCreatedAtDesc(UUID orgId);
    List<Inquiry> findByOrgIdAndStatusOrderByCreatedAtDesc(UUID orgId, InquiryStatus status);
    List<Inquiry> findAllByOrderByCreatedAtDesc();
    List<Inquiry> findAllByStatusOrderByCreatedAtDesc(InquiryStatus status);
}

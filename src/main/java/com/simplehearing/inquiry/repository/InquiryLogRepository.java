package com.simplehearing.inquiry.repository;

import com.simplehearing.inquiry.entity.InquiryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InquiryLogRepository extends JpaRepository<InquiryLog, UUID> {
    List<InquiryLog> findByInquiryIdOrderByCreatedAtAsc(UUID inquiryId);

    List<InquiryLog> findByInquiryIdInOrderByCreatedAtAsc(List<UUID> inquiryIds);
}

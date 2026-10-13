package com.simplehearing.auditlog.repository;

import com.simplehearing.auditlog.entity.AuditLogEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, UUID> {

    List<AuditLogEntry> findByOrgIdAndSubjectTypeAndSubjectIdOrderByCreatedAtDesc(
            UUID orgId, String subjectType, UUID subjectId, Pageable pageable);
}

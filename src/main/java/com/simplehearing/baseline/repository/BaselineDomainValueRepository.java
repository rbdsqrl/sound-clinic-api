package com.simplehearing.baseline.repository;

import com.simplehearing.baseline.entity.BaselineDomainValue;
import com.simplehearing.baseline.enums.BaselineDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BaselineDomainValueRepository extends JpaRepository<BaselineDomainValue, UUID> {
    List<BaselineDomainValue> findByReportId(UUID reportId);
    Optional<BaselineDomainValue> findByReportIdAndDomain(UUID reportId, BaselineDomain domain);
}

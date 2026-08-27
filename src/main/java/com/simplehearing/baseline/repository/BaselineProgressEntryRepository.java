package com.simplehearing.baseline.repository;

import com.simplehearing.baseline.entity.BaselineProgressEntry;
import com.simplehearing.baseline.enums.BaselineDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BaselineProgressEntryRepository extends JpaRepository<BaselineProgressEntry, UUID> {
    /** Every entry for a report, across all domains — used to build the full per-domain view in one query. */
    List<BaselineProgressEntry> findByReportIdOrderByEntryDateDesc(UUID reportId);

    List<BaselineProgressEntry> findByReportIdAndDomainOrderByEntryDateDesc(UUID reportId, BaselineDomain domain);
}

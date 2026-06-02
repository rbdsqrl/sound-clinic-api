package com.simplehearing.tax.repository;

import com.simplehearing.tax.entity.Tax;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaxRepository extends JpaRepository<Tax, UUID> {

    List<Tax> findByOrgIdAndIsActiveTrueOrderByNameAsc(UUID orgId);
}

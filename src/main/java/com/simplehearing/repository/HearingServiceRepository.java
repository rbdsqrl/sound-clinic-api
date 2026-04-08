package com.simplehearing.repository;

import com.simplehearing.entity.HearingService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HearingServiceRepository extends JpaRepository<HearingService, Long> {
    List<HearingService> findByActiveTrueOrderByDisplayOrderAsc();
}

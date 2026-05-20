package com.simplehearing.holiday.repository;

import com.simplehearing.holiday.entity.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, UUID> {

    List<PublicHoliday> findByOrgIdOrderByHolidayDateAsc(UUID orgId);

    List<PublicHoliday> findByOrgIdAndHolidayDateBetweenOrderByHolidayDateAsc(
            UUID orgId, LocalDate from, LocalDate to);

    Optional<PublicHoliday> findByOrgIdAndHolidayDate(UUID orgId, LocalDate date);
}

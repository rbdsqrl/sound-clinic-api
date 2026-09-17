package com.simplehearing.calendarblock.repository;

import com.simplehearing.calendarblock.entity.OrgCalendarBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrgCalendarBlockRepository extends JpaRepository<OrgCalendarBlock, UUID> {

    List<OrgCalendarBlock> findByOrgIdOrderByStartTimeAsc(UUID orgId);
}

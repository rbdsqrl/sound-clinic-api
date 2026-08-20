package com.simplehearing.meeting.repository;

import com.simplehearing.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    List<Meeting> findByOrgIdAndMeetingDateBetweenOrderByMeetingDateAscStartTimeAsc(
            UUID orgId, LocalDate from, LocalDate to);

    /** Meetings in the window that the user either created or was invited to. */
    @Query("""
           SELECT DISTINCT m FROM Meeting m LEFT JOIN m.participantIds p
           WHERE m.orgId = :orgId
             AND m.meetingDate BETWEEN :from AND :to
             AND (m.createdBy = :userId OR p = :userId)
           ORDER BY m.meetingDate ASC, m.startTime ASC
           """)
    List<Meeting> findVisibleTo(@Param("orgId") UUID orgId,
                                @Param("userId") UUID userId,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to);
}

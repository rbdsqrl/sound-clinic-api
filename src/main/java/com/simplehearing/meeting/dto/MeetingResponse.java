package com.simplehearing.meeting.dto;

import com.simplehearing.common.dto.ParticipantResponse;
import com.simplehearing.meeting.entity.Meeting;
import com.simplehearing.meeting.enums.MeetingStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record MeetingResponse(
        UUID id,
        UUID orgId,
        String title,
        String description,
        LocalDate meetingDate,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        MeetingStatus status,
        String cancelledReason,
        UUID createdBy,
        String createdByName,
        List<ParticipantResponse> participants
) {
    public static MeetingResponse from(Meeting m,
                                       String createdByName,
                                       List<ParticipantResponse> participants) {
        return new MeetingResponse(
                m.getId(), m.getOrgId(), m.getTitle(), m.getDescription(),
                m.getMeetingDate(), m.getStartTime(), m.getEndTime(), m.getLocation(),
                m.getStatus(), m.getCancelledReason(),
                m.getCreatedBy(), createdByName, participants);
    }
}

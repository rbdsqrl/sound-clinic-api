package com.simplehearing.meeting.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateMeetingRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200)
        String title,

        String description,

        @NotNull(message = "Date is required")
        LocalDate meetingDate,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        @Size(max = 255)
        String location,

        /** Everyone invited. The organiser is added automatically. */
        @NotNull @NotEmpty(message = "Pick at least one participant")
        List<UUID> participantIds
) {}

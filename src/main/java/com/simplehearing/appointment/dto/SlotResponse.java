package com.simplehearing.appointment.dto;

import com.simplehearing.appointment.entity.TherapistSlot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record SlotResponse(
        UUID id,
        UUID therapistId,
        String therapistFirstName,
        String therapistLastName,
        UUID clinicId,
        String clinicName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes
) {
    public static SlotResponse from(TherapistSlot s, String therapistFirstName, String therapistLastName, String clinicName) {
        return new SlotResponse(
                s.getId(), s.getTherapistId(), therapistFirstName, therapistLastName,
                s.getClinicId(), clinicName,
                s.getDayOfWeek(), s.getStartTime(), s.getEndTime(), s.getSlotDurationMinutes()
        );
    }
}

package com.simplehearing.service;

import com.simplehearing.dto.response.AvailabilityResponse;
import com.simplehearing.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    private static final List<String> ALL_SLOTS = List.of(
        "9:00 AM", "9:30 AM", "10:00 AM", "10:30 AM",
        "11:00 AM", "11:30 AM", "2:00 PM", "2:30 PM",
        "3:00 PM", "3:30 PM", "4:00 PM", "4:30 PM"
    );

    private static final DateTimeFormatter DAY_LABEL_FORMAT =
        DateTimeFormatter.ofPattern("EEE, MMM d");

    private final AppointmentRepository appointmentRepo;

    public AvailabilityService(AppointmentRepository appointmentRepo) {
        this.appointmentRepo = appointmentRepo;
    }

    public List<AvailabilityResponse> getAvailabilityRange(LocalDate from, int days) {
        List<AvailabilityResponse> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = from.plusDays(i);
            // Skip Sundays
            if (date.getDayOfWeek().getValue() == 7) continue;
            result.add(getForDate(date));
        }
        return result;
    }

    public AvailabilityResponse getForDate(LocalDate date) {
        Set<String> booked = Set.copyOf(appointmentRepo.findBookedSlotsByDate(date));

        // Saturdays have limited slots (9 AM – 12 PM)
        List<String> daySlots = date.getDayOfWeek().getValue() == 6
            ? ALL_SLOTS.subList(0, 6)
            : ALL_SLOTS;

        List<String> available = daySlots.stream()
            .filter(slot -> !booked.contains(slot))
            .toList();

        return new AvailabilityResponse(
            date.toString(),
            date.format(DAY_LABEL_FORMAT),
            available
        );
    }
}

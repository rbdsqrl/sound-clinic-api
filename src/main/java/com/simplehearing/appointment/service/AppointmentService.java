package com.simplehearing.appointment.service;

import com.simplehearing.appointment.dto.*;
import com.simplehearing.appointment.entity.Appointment;
import com.simplehearing.appointment.entity.TherapistSlot;
import com.simplehearing.appointment.enums.AppointmentStatus;
import com.simplehearing.appointment.repository.AppointmentRepository;
import com.simplehearing.appointment.repository.TherapistSlotRepository;
import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.clinic.entity.Clinic;
import com.simplehearing.clinic.repository.ClinicRepository;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.patient.entity.Patient;
import com.simplehearing.patient.repository.PatientRepository;
import com.simplehearing.patient.repository.TherapistPatientRepository;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppointmentService {

    private final TherapistSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final TherapistPatientRepository therapistPatientRepository;

    public AppointmentService(TherapistSlotRepository slotRepository,
                              AppointmentRepository appointmentRepository,
                              UserRepository userRepository,
                              PatientRepository patientRepository,
                              ClinicRepository clinicRepository,
                              TherapistPatientRepository therapistPatientRepository) {
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.clinicRepository = clinicRepository;
        this.therapistPatientRepository = therapistPatientRepository;
    }

    // ── Therapist Slots ───────────────────────────────────────────────────────

    public SlotResponse createSlot(CreateSlotRequest req, UserPrincipal principal) {
        // Validate therapist exists in org
        User therapist = userRepository.findById(req.therapistId())
                .filter(u -> u.getOrgId().equals(principal.getOrgId()))
                .filter(u -> u.hasRole(Role.THERAPIST) || u.hasRole(Role.DOCTOR))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Therapist not found in your organisation"));

        // Validate clinic exists in org
        Clinic clinic = clinicRepository.findByIdAndOrgId(req.clinicId(), principal.getOrgId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Clinic not found in your organisation"));

        if (!req.endTime().isAfter(req.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }

        if (req.slotDurationMinutes() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Slot duration must be positive");
        }

        TherapistSlot slot = new TherapistSlot();
        slot.setOrgId(principal.getOrgId());
        slot.setTherapistId(req.therapistId());
        slot.setClinicId(req.clinicId());
        slot.setDayOfWeek(req.dayOfWeek());
        slot.setStartTime(req.startTime());
        slot.setEndTime(req.endTime());
        slot.setSlotDurationMinutes(req.slotDurationMinutes());

        TherapistSlot saved = slotRepository.save(slot);
        return SlotResponse.from(saved, therapist.getFirstName(), therapist.getLastName(), clinic.getName());
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> listSlots(UUID therapistId, UserPrincipal principal) {
        List<TherapistSlot> slots = therapistId != null
                ? slotRepository.findByTherapistIdAndOrgId(therapistId, principal.getOrgId())
                : slotRepository.findByOrgId(principal.getOrgId());

        // Build lookup maps
        List<UUID> therapistIds = slots.stream().map(TherapistSlot::getTherapistId).distinct().toList();
        List<UUID> clinicIds    = slots.stream().map(TherapistSlot::getClinicId).distinct().toList();

        Map<UUID, User>   therapists = userRepository.findAllById(therapistIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, Clinic> clinics    = clinicRepository.findAllById(clinicIds).stream()
                .collect(Collectors.toMap(Clinic::getId, Function.identity()));

        return slots.stream().map(s -> {
            User   t = therapists.get(s.getTherapistId());
            Clinic c = clinics.get(s.getClinicId());
            return SlotResponse.from(s,
                    t != null ? t.getFirstName() : "Unknown",
                    t != null ? t.getLastName()  : "",
                    c != null ? c.getName()       : "Unknown");
        }).toList();
    }

    public void deleteSlot(UUID slotId, UserPrincipal principal) {
        TherapistSlot slot = slotRepository.findByIdAndOrgId(slotId, principal.getOrgId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Slot not found"));
        slotRepository.delete(slot);
    }

    // ── Appointments ──────────────────────────────────────────────────────────

    public AppointmentResponse book(BookAppointmentRequest req, UserPrincipal principal) {
        // Patient must belong to org
        Patient patient = patientRepository.findById(req.patientId())
                .filter(p -> p.getOrgId().equals(principal.getOrgId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));

        // If caller is a PARENT, they must be linked to this patient
        // Therapist must be assigned to the patient (active assignment)
        boolean therapistAssigned = therapistPatientRepository
                .findByPatientIdAndIsActive(req.patientId(), true)
                .stream().anyMatch(tp -> tp.getTherapistId().equals(req.therapistId()));
        if (!therapistAssigned) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Therapist is not assigned to this patient");
        }

        User therapist = userRepository.findById(req.therapistId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Therapist not found"));

        // Find the slot that covers this day + time
        List<TherapistSlot> slots = slotRepository.findByTherapistIdAndOrgId(req.therapistId(), principal.getOrgId());
        TherapistSlot matchingSlot = slots.stream()
                .filter(s -> s.getDayOfWeek() == req.appointmentDate().getDayOfWeek())
                .filter(s -> !req.startTime().isBefore(s.getStartTime()))
                .filter(s -> {
                    LocalTime slotEnd = req.startTime().plusMinutes(s.getSlotDurationMinutes());
                    return !slotEnd.isAfter(s.getEndTime());
                })
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "No availability slot covers the requested date/time"));

        // Clash check
        if (appointmentRepository.existsByTherapistIdAndAppointmentDateAndStartTime(
                req.therapistId(), req.appointmentDate(), req.startTime())) {
            throw new ApiException(HttpStatus.CONFLICT, "This slot has already been booked");
        }

        Clinic clinic = clinicRepository.findById(matchingSlot.getClinicId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Clinic not found"));

        Appointment appt = new Appointment();
        appt.setOrgId(principal.getOrgId());
        appt.setPatientId(req.patientId());
        appt.setTherapistId(req.therapistId());
        appt.setClinicId(matchingSlot.getClinicId());
        appt.setBookedBy(principal.getId());
        appt.setAppointmentDate(req.appointmentDate());
        appt.setStartTime(req.startTime());
        appt.setEndTime(req.startTime().plusMinutes(matchingSlot.getSlotDurationMinutes()));
        appt.setStatus(AppointmentStatus.PENDING);
        appt.setNotes(req.notes());

        Appointment saved = appointmentRepository.save(appt);
        return AppointmentResponse.from(saved,
                patient.getFirstName(), patient.getLastName(),
                therapist.getFirstName(), therapist.getLastName(),
                clinic.getName());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listForCaller(UserPrincipal principal) {
        List<Appointment> appts;
        Role role = principal.getUser().getRole();

        if (role == Role.THERAPIST || role == Role.DOCTOR) {
            appts = appointmentRepository.findByTherapistIdAndOrgIdOrderByAppointmentDateAscStartTimeAsc(
                    principal.getId(), principal.getOrgId());
        } else if (role == Role.PARENT) {
            appts = appointmentRepository.findByBookedByAndOrgIdOrderByAppointmentDateAscStartTimeAsc(
                    principal.getId(), principal.getOrgId());
        } else {
            // BUSINESS_OWNER / ADMIN — see everything
            appts = appointmentRepository.findByOrgIdOrderByAppointmentDateAscStartTimeAsc(principal.getOrgId());
        }

        return enrich(appts);
    }

    public AppointmentResponse updateStatus(UUID apptId, UpdateAppointmentStatusRequest req, UserPrincipal principal) {
        Appointment appt = appointmentRepository.findByIdAndOrgId(apptId, principal.getOrgId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appointment not found"));

        Role role = principal.getUser().getRole();

        // Therapists can confirm or cancel their own appointments
        if (role == Role.THERAPIST || role == Role.DOCTOR) {
            if (!appt.getTherapistId().equals(principal.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Not your appointment");
            }
        }
        // Parents can only cancel appointments they booked
        if (role == Role.PARENT) {
            if (!appt.getBookedBy().equals(principal.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Not your appointment");
            }
            if (req.status() != AppointmentStatus.CANCELLED) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Parents may only cancel appointments");
            }
        }

        appt.setStatus(req.status());
        Appointment saved = appointmentRepository.save(appt);
        return enrichOne(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<AppointmentResponse> enrich(List<Appointment> appts) {
        List<UUID> patientIds   = appts.stream().map(Appointment::getPatientId).distinct().toList();
        List<UUID> therapistIds = appts.stream().map(Appointment::getTherapistId).distinct().toList();
        List<UUID> clinicIds    = appts.stream().map(Appointment::getClinicId).distinct().toList();

        Map<UUID, Patient> patients   = patientRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, Function.identity()));
        Map<UUID, User>    therapists = userRepository.findAllById(therapistIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, Clinic>  clinics    = clinicRepository.findAllById(clinicIds).stream()
                .collect(Collectors.toMap(Clinic::getId, Function.identity()));

        return appts.stream().map(a -> {
            Patient p = patients.get(a.getPatientId());
            User    t = therapists.get(a.getTherapistId());
            Clinic  c = clinics.get(a.getClinicId());
            return AppointmentResponse.from(a,
                    p != null ? p.getFirstName() : "Unknown", p != null ? p.getLastName() : "",
                    t != null ? t.getFirstName() : "Unknown", t != null ? t.getLastName()  : "",
                    c != null ? c.getName()       : "Unknown");
        }).toList();
    }

    private AppointmentResponse enrichOne(Appointment a) {
        Patient p = patientRepository.findById(a.getPatientId()).orElse(null);
        User    t = userRepository.findById(a.getTherapistId()).orElse(null);
        Clinic  c = clinicRepository.findById(a.getClinicId()).orElse(null);
        return AppointmentResponse.from(a,
                p != null ? p.getFirstName() : "Unknown", p != null ? p.getLastName() : "",
                t != null ? t.getFirstName() : "Unknown", t != null ? t.getLastName()  : "",
                c != null ? c.getName()       : "Unknown");
    }
}

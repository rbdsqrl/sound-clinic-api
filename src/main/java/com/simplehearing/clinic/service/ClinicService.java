package com.simplehearing.clinic.service;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.clinic.dto.ClinicResponse;
import com.simplehearing.clinic.dto.CreateClinicRequest;
import com.simplehearing.clinic.entity.Clinic;
import com.simplehearing.clinic.repository.ClinicRepository;
import com.simplehearing.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClinicService {

    private final ClinicRepository clinicRepository;

    public ClinicService(ClinicRepository clinicRepository) {
        this.clinicRepository = clinicRepository;
    }

    public ClinicResponse create(CreateClinicRequest request, UserPrincipal principal) {
        Clinic clinic = new Clinic();
        clinic.setOrgId(principal.getOrgId());
        clinic.setName(request.name());
        if (request.address() != null)  clinic.setAddress(request.address());
        if (request.phone() != null)    clinic.setPhone(request.phone());
        if (request.email() != null)    clinic.setEmail(request.email());
        if (request.timezone() != null) clinic.setTimezone(request.timezone());
        return ClinicResponse.from(clinicRepository.save(clinic));
    }

    @Transactional(readOnly = true)
    public List<ClinicResponse> listForOrg(UserPrincipal principal) {
        return clinicRepository.findByOrgId(principal.getOrgId()).stream()
                .map(ClinicResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClinicResponse get(UUID clinicId, UserPrincipal principal) {
        return clinicRepository.findByIdAndOrgId(clinicId, principal.getOrgId())
                .map(ClinicResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Clinic not found"));
    }

    public ClinicResponse update(UUID clinicId, CreateClinicRequest request, UserPrincipal principal) {
        Clinic clinic = clinicRepository.findByIdAndOrgId(clinicId, principal.getOrgId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Clinic not found"));

        if (request.name() != null)     clinic.setName(request.name());
        if (request.address() != null)  clinic.setAddress(request.address());
        if (request.phone() != null)    clinic.setPhone(request.phone());
        if (request.email() != null)    clinic.setEmail(request.email());
        if (request.timezone() != null) clinic.setTimezone(request.timezone());

        return ClinicResponse.from(clinicRepository.save(clinic));
    }
}

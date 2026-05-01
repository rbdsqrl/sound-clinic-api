package com.simplehearing.organisation.service;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.organisation.dto.OrganisationResponse;
import com.simplehearing.organisation.dto.UpdateOrganisationRequest;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrganisationService {

    private final OrganisationRepository organisationRepository;

    public OrganisationService(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }

    public OrganisationResponse getMyOrg(UserPrincipal principal) {
        Organisation org = organisationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organisation not found"));
        return OrganisationResponse.from(org);
    }

    public OrganisationResponse updateMyOrg(UpdateOrganisationRequest request, UserPrincipal principal) {
        Organisation org = organisationRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organisation not found"));

        if (request.name() != null)         org.setName(request.name());
        if (request.contactEmail() != null) org.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null) org.setContactPhone(request.contactPhone());
        if (request.address() != null)      org.setAddress(request.address());
        if (request.logoUrl() != null)      org.setLogoUrl(request.logoUrl());
        if (request.timezone() != null)     org.setTimezone(request.timezone());

        return OrganisationResponse.from(organisationRepository.save(org));
    }
}

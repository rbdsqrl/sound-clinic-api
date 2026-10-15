package com.simplehearing.customrole.service;

import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.customrole.entity.OrgCustomRole;
import com.simplehearing.customrole.repository.OrgCustomRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The organisation's list of Custom Role names: normalising a typed name and adding it to the list once. */
@Service
public class OrgCustomRoleService {

    public static final int MAX_NAME = 100;

    private final OrgCustomRoleRepository repository;

    public OrgCustomRoleService(OrgCustomRoleRepository repository) {
        this.repository = repository;
    }

    public List<OrgCustomRole> list(UUID orgId) {
        return repository.findByOrgIdOrderByNameAsc(orgId);
    }

    /** Trims, collapses runs of whitespace, and drops control characters. */
    public static String clean(String raw) {
        return raw == null ? "" : raw.replaceAll("\\p{Cntrl}", " ").trim().replaceAll("\\s+", " ");
    }

    /** Adds the name to the organisation's list if it isn't there already (case-insensitive); returns the stored entry. */
    @Transactional
    public OrgCustomRole findOrCreate(UUID orgId, String rawName, UUID createdBy) {
        String name = clean(rawName);
        if (name.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a name for the custom role");
        if (name.length() > MAX_NAME) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A custom role can be at most " + MAX_NAME + " characters");
        }

        Optional<OrgCustomRole> existing = repository.findByOrgIdAndNameIgnoreCase(orgId, name);
        if (existing.isPresent()) return existing.get();

        OrgCustomRole role = new OrgCustomRole();
        role.setOrgId(orgId);
        role.setName(name);
        role.setCreatedBy(createdBy);
        return repository.save(role);
    }

    /** Removes a name from the picker. Members who already have it keep it. */
    @Transactional
    public void remove(UUID orgId, UUID id) {
        repository.delete(repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Custom role not found")));
    }
}

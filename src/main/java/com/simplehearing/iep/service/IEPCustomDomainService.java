package com.simplehearing.iep.service;

import com.simplehearing.common.exception.ApiException;
import com.simplehearing.iep.entity.IEPCustomDomain;
import com.simplehearing.iep.enums.IEPGoalDomain;
import com.simplehearing.iep.repository.IEPCustomDomainRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The organisation's own IEP goal domains: normalising a typed name, and adding it to the shared list once. */
@Service
public class IEPCustomDomainService {

    public static final int MAX_NAME = 60;

    /** A goal's domain as stored: the enum value plus, for CUSTOM, the (canonical) custom name. */
    public record Resolved(IEPGoalDomain domain, String customDomain) {}

    private final IEPCustomDomainRepository repository;

    public IEPCustomDomainService(IEPCustomDomainRepository repository) {
        this.repository = repository;
    }

    public List<IEPCustomDomain> list(UUID orgId) {
        return repository.findByOrgIdOrderByNameAsc(orgId);
    }

    /** Trims, collapses runs of whitespace, and drops control characters. */
    static String clean(String raw) {
        return raw == null ? "" : raw.replaceAll("\\p{Cntrl}", " ").trim().replaceAll("\\s+", " ");
    }

    /** Adds the domain to the organisation's list if it isn't there already (case-insensitive); returns the stored entry. */
    @Transactional
    public IEPCustomDomain findOrCreate(UUID orgId, String rawName, UUID createdBy) {
        String name = clean(rawName);
        if (name.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a name for the custom domain");
        if (name.length() > MAX_NAME) throw new ApiException(HttpStatus.BAD_REQUEST, "A domain name can be at most " + MAX_NAME + " characters");

        Optional<IEPCustomDomain> existing = repository.findByOrgIdAndNameIgnoreCase(orgId, name);
        if (existing.isPresent()) return existing.get();

        IEPCustomDomain d = new IEPCustomDomain();
        d.setOrgId(orgId);
        d.setName(name);
        d.setCreatedBy(createdBy);
        return repository.save(d);
    }

    /**
     * Turns the domain a client sent into what gets stored. A CUSTOM domain needs a name, and is added to the
     * organisation's list on the way (so typing a new one while creating a goal both defines and uses it). A name
     * that is really a built-in domain ("speech") resolves to that built-in instead of creating a lookalike.
     */
    @Transactional
    public Resolved resolve(UUID orgId, UUID userId, IEPGoalDomain domain, String customDomain) {
        if (domain != IEPGoalDomain.CUSTOM) return new Resolved(domain, null);

        String name = clean(customDomain);
        for (IEPGoalDomain built : IEPGoalDomain.values()) {
            if (built != IEPGoalDomain.CUSTOM && built.name().equalsIgnoreCase(name)) return new Resolved(built, null);
        }
        return new Resolved(IEPGoalDomain.CUSTOM, findOrCreate(orgId, name, userId).getName());
    }

    /** Removes a name from the picker list. Goals already using it keep their domain name. */
    @Transactional
    public void remove(UUID orgId, UUID id) {
        repository.delete(repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new com.simplehearing.common.exception.ResourceNotFoundException("Custom domain not found")));
    }
}

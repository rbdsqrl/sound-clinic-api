package com.simplehearing.service;

import com.simplehearing.dto.response.HearingServiceResponse;
import com.simplehearing.entity.HearingService;
import com.simplehearing.exception.ResourceNotFoundException;
import com.simplehearing.repository.HearingServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HearingServiceService {

    private final HearingServiceRepository repo;

    public HearingServiceService(HearingServiceRepository repo) {
        this.repo = repo;
    }

    public List<HearingServiceResponse> getAllActive() {
        return repo.findByActiveTrueOrderByDisplayOrderAsc()
            .stream()
            .map(HearingServiceResponse::from)
            .toList();
    }

    public HearingServiceResponse getById(Long id) {
        return repo.findById(id)
            .filter(HearingService::isActive)
            .map(HearingServiceResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("Service", id));
    }

    @Transactional
    public HearingServiceResponse create(HearingService service) {
        return HearingServiceResponse.from(repo.save(service));
    }

    @Transactional
    public HearingServiceResponse update(Long id, HearingService updated) {
        HearingService existing = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service", id));
        existing.setName(updated.getName());
        existing.setShortDescription(updated.getShortDescription());
        existing.setFullDescription(updated.getFullDescription());
        existing.setWhatWeAddress(updated.getWhatWeAddress());
        existing.setPriceInr(updated.getPriceInr());
        existing.setDurationMinutes(updated.getDurationMinutes());
        existing.setDisplayOrder(updated.getDisplayOrder());
        return HearingServiceResponse.from(repo.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        HearingService service = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service", id));
        service.setActive(false);
        repo.save(service);
    }
}

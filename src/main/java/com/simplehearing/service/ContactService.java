package com.simplehearing.service;

import com.simplehearing.dto.request.ContactMessageRequest;
import com.simplehearing.dto.response.ContactMessageResponse;
import com.simplehearing.entity.ContactMessage;
import com.simplehearing.exception.ResourceNotFoundException;
import com.simplehearing.repository.ContactMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContactService {

    private final ContactMessageRepository repo;

    public ContactService(ContactMessageRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ContactMessageResponse submit(ContactMessageRequest request) {
        ContactMessage msg = new ContactMessage();
        msg.setFullName(request.fullName());
        msg.setEmail(request.email());
        msg.setPhone(request.phone());
        msg.setMessage(request.message());
        return ContactMessageResponse.from(repo.save(msg));
    }

    public Page<ContactMessageResponse> getAll(Boolean readFilter, Pageable pageable) {
        if (readFilter != null) {
            return repo.findByReadOrderBySubmittedAtDesc(readFilter, pageable)
                .map(ContactMessageResponse::from);
        }
        return repo.findAllByOrderBySubmittedAtDesc(pageable)
            .map(ContactMessageResponse::from);
    }

    @Transactional
    public ContactMessageResponse markRead(Long id) {
        ContactMessage msg = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Contact message", id));
        msg.setRead(true);
        return ContactMessageResponse.from(repo.save(msg));
    }

    public long countUnread() {
        return repo.countByReadFalse();
    }
}

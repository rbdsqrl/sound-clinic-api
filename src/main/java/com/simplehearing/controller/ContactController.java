package com.simplehearing.controller;

import com.simplehearing.dto.request.ContactMessageRequest;
import com.simplehearing.dto.response.ContactMessageResponse;
import com.simplehearing.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contact")
@Tag(name = "Contact", description = "Contact form submissions")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @Operation(summary = "Submit a contact message")
    public ResponseEntity<ContactMessageResponse> submit(
        @Valid @RequestBody ContactMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(contactService.submit(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all contact messages (admin)")
    public ResponseEntity<Page<ContactMessageResponse>> getAll(
        @RequestParam(required = false) Boolean read,
        @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(contactService.getAll(read, pageable));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark a message as read (admin)")
    public ResponseEntity<ContactMessageResponse> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.markRead(id));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get unread message count (admin)")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", contactService.countUnread()));
    }
}

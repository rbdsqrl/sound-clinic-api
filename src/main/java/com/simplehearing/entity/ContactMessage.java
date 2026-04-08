package com.simplehearing.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages")
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 2000)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false)
    private boolean read = false;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }

    // Constructors
    public ContactMessage() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}

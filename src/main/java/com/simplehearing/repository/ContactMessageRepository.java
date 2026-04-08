package com.simplehearing.repository;

import com.simplehearing.entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    Page<ContactMessage> findAllByOrderBySubmittedAtDesc(Pageable pageable);
    Page<ContactMessage> findByReadOrderBySubmittedAtDesc(boolean read, Pageable pageable);
    long countByReadFalse();
}

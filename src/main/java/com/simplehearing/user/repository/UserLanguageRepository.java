package com.simplehearing.user.repository;

import com.simplehearing.user.entity.UserLanguage;
import com.simplehearing.user.entity.UserLanguageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface UserLanguageRepository extends JpaRepository<UserLanguage, UserLanguageId> {

    List<UserLanguage> findById_UserId(UUID userId);

    @Transactional
    void deleteById_UserId(UUID userId);
}

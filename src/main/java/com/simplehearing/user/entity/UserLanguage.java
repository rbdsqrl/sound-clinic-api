package com.simplehearing.user.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_languages")
public class UserLanguage {

    @EmbeddedId
    private UserLanguageId id;

    public UserLanguage() {}

    public UserLanguage(UUID userId, UUID languageId) {
        this.id = new UserLanguageId(userId, languageId);
    }

    public UserLanguageId getId() { return id; }
    public UUID getUserId() { return id.getUserId(); }
    public UUID getLanguageId() { return id.getLanguageId(); }
}

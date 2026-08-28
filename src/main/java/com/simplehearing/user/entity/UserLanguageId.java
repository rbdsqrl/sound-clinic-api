package com.simplehearing.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserLanguageId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "language_id")
    private UUID languageId;

    public UserLanguageId() {}

    public UserLanguageId(UUID userId, UUID languageId) {
        this.userId = userId;
        this.languageId = languageId;
    }

    public UUID getUserId() { return userId; }
    public UUID getLanguageId() { return languageId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserLanguageId)) return false;
        UserLanguageId that = (UserLanguageId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(languageId, that.languageId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, languageId); }
}

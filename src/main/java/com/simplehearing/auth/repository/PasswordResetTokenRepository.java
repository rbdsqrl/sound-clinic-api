package com.simplehearing.auth.repository;

import com.simplehearing.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Issuing a new token voids every earlier unused one for that user.
     * Voided rather than deleted — the rows are what the rate-limit counters below count,
     * so removing them would let a caller reset the window simply by asking again.
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.userId = :userId AND t.usedAt IS NULL")
    void voidUnusedByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.userId = :userId AND t.createdAt > :since")
    long countByUserIdSince(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.requestedIp = :ip AND t.createdAt > :since")
    long countByRequestedIpSince(@Param("ip") String ip, @Param("since") Instant since);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}

package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.UserSession;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<UserSession, UUID> {

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE user_sessions
            SET revoked_at = CURRENT_TIMESTAMP
            WHERE token = :tokenHash
              AND revoked_at IS NULL
            """, nativeQuery = true)
    int revokeSession(@Param("tokenHash") String tokenHash);

    @Query(value = """
        SELECT *
        FROM user_sessions
        WHERE token_hash = :tokenHash
          AND revoked_at IS NULL
        """, nativeQuery = true)
    Optional<UserSession> findActiveSession(
            @Param("tokenHash") String tokenHash);
}

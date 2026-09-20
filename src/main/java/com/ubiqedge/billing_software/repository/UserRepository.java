package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);

    @Query(value = """
            SELECT *
            FROM users
            WHERE username = :username
              AND deleted_at IS NULL
            """, nativeQuery = true)
    Optional<User> findActiveUserByName(
            @Param("username") String username
    );

    @Query(value = """
            SELECT *
            FROM users
            WHERE id = :id
              AND deleted_at IS NULL
            """, nativeQuery = true)
    Optional<User> findActiveUserById(
            @Param("id") UUID id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u
            FROM User u
            WHERE u.id = :id
              AND u.deletedAt IS NULL
            """)
    Optional<User> findActiveUserByIdForUpdate(
            @Param("id") UUID id
    );
}

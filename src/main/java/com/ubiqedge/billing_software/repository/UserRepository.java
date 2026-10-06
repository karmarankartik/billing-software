package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    boolean existsByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    List<User> findAllByRoleAndDeletedAtIsNullOrderByUsernameAsc(
        String role
);
}

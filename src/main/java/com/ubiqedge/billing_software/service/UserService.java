package com.ubiqedge.billing_software.service;

import com.password4j.Password;
import com.ubiqedge.billing_software.dto.CreateUserRequest;
import com.ubiqedge.billing_software.dto.UpdateUserRequest;
import com.ubiqedge.billing_software.dto.UserResponse;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(
            UserSession userSession,
            User loggedInUser,
            CreateUserRequest request) {

        validateAdmin(userSession, loggedInUser);
        validateRole(request.role());

        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(
                    USERNAME_ALREADY_EXISTS,
                    HttpStatus.BAD_REQUEST
            );
        }

        Instant now = Instant.now();

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(
                Password.hash(request.password())
                        .withArgon2()
                        .getResult()
        );
        user.setRole(request.role());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeletedAt(null);

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public UserResponse getUser(
            UserSession userSession,
            User loggedInUser,
            UUID id) {

        validateAdmin(userSession, loggedInUser);

        User user = userRepository.findActiveUserById(id)
                .orElseThrow(() -> new ApiException(
                        USER_NOT_FOUND,
                        HttpStatus.BAD_REQUEST
                ));

        return toResponse(user);
    }

    public UserResponse updateUser(
            UserSession userSession,
            User loggedInUser,
            UUID id,
            UpdateUserRequest request) {

        validateAdmin(userSession, loggedInUser);
        validateRole(request.role());

        User user = userRepository.findActiveUserById(id)
                .orElseThrow(() -> new ApiException(
                        USER_NOT_FOUND,
                        HttpStatus.BAD_REQUEST
                ));

        if (!user.getUsername().equals(request.username())
                && userRepository.existsByUsername(request.username())) {

            throw new ApiException(
                    USERNAME_ALREADY_EXISTS,
                    HttpStatus.BAD_REQUEST
            );
        }

        user.setUsername(request.username());
        user.setRole(request.role());
        user.setUpdatedAt(Instant.now());

        if (request.password() != null
                && !request.password().isBlank()) {

            user.setPasswordHash(
                    Password.hash(request.password())
                            .withArgon2()
                            .getResult()
            );
        }

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }

    public void deleteUser(
            UserSession userSession,
            User loggedInUser,
            UUID id) {

        validateAdmin(userSession, loggedInUser);

        User user = userRepository.findActiveUserById(id)
                .orElseThrow(() -> new ApiException(
                        USER_NOT_FOUND,
                        HttpStatus.BAD_REQUEST
                ));

        user.setDeletedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
    }

    private void validateAdmin(
            UserSession userSession,
            User loggedInUser) {

        if (userSession == null || loggedInUser == null) {
            throw new ApiException(
                    INVALID_SESSION,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!ROLE_ADMIN.equals(loggedInUser.getRole())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED.toString(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateRole(String role) {

        if (!ROLE_ADMIN.equals(role)
                && !ROLE_USER.equals(role)) {

            throw new ApiException(
                    INVALID_ROLE_TYPE,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
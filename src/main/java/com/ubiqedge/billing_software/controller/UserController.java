package com.ubiqedge.billing_software.controller;

import com.ubiqedge.billing_software.dto.ApiResponse;
import com.ubiqedge.billing_software.dto.CreateUserRequest;
import com.ubiqedge.billing_software.dto.UpdateUserRequest;
import com.ubiqedge.billing_software.dto.UserResponse;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.ubiqedge.billing_software.constant.AppConstant.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @RequestBody CreateUserRequest request) {

        UserResponse response = userService.createUser(
                userSession,
                user,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        USER_CREATED_SUCCESSFULLY,
                        HttpStatus.OK,
                        response
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID id) {

        UserResponse response = userService.getUser(
                userSession,
                user,
                id
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        response
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request) {

        UserResponse response = userService.updateUser(
                userSession,
                user,
                id,
                request
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        response
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID id) {

        userService.deleteUser(
                userSession,
                user,
                id
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        null
                )
        );
    }
}
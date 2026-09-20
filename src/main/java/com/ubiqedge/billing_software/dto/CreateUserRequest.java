package com.ubiqedge.billing_software.dto;

public record CreateUserRequest(
        String username,
        String password,
        String role
) {
}
package com.ubiqedge.billing_software.dto;

public record UpdateUserRequest(
        String username,
        String password,
        String role
) {
}
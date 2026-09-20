package com.ubiqedge.billing_software.dto;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(boolean success, String errorMessage, HttpStatus httpStatusCode, T data) {
}

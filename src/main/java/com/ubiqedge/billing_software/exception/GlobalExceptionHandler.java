package com.ubiqedge.billing_software.exception;

import com.ubiqedge.billing_software.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse> handleApiException(
            ApiException exception) {

        return ResponseEntity
                .status(exception.getStatus())
                .body(new ApiResponse(
                        Boolean.FALSE,
                        exception.getMessage(),
                        exception.getStatus(),
                        null
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception) {

        String message = GENRIC_ERROR_MESSAGE;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        String databaseMessage = exception.getMostSpecificCause()
                .getMessage();

        if (databaseMessage != null) {

            if (databaseMessage.contains(
                    "FK_WATER_METER_READINGS_METER")) {

                message = INVALID_WATER_METER_ID;
                status = HttpStatus.BAD_REQUEST;

            } else if (databaseMessage.contains(
                    "UK_WATER_METER_READINGS_INGESTION")) {

                message = DUPLICATE_WATER_METER_INGESTION;
                status = HttpStatus.BAD_REQUEST;

            } else if (databaseMessage.contains(
                    "UK_WATER_METER_READINGS_METER_TYPE_TIME")) {

                message = DUPLICATE_WATER_METER_READING;
                status = HttpStatus.BAD_REQUEST;

            } else if (databaseMessage.contains(
                    "CK_WATER_METER_READINGS_VALUE")) {

                message = NEGATIVE_VALUE_NOT_ALLOWED;
                status = HttpStatus.BAD_REQUEST;
            }
        }

        return ResponseEntity
                .status(status)
                .body(new ApiResponse(
                        Boolean.FALSE,
                        message,
                        status,
                        null
                ));
    }
}


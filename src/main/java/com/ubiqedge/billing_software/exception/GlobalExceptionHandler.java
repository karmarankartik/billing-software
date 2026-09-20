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

    @ExceptionHandler(ApiException.class) public ResponseEntity<ApiResponse>
    handleApiException(ApiException exception){
        return ResponseEntity.status(exception.getStatus()).body(new ApiResponse(Boolean.FALSE,exception.getMessage(),exception.getStatus(),null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse>
    handleDataIntegrityViolationException(
            DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(Boolean.FALSE,GENRIC_ERROR_MESSAGE,HttpStatus.INTERNAL_SERVER_ERROR,null));

    }


}
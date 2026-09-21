package com.ubiqedge.billing_software.controller;

import com.ubiqedge.billing_software.dto.ApiResponse;
import com.ubiqedge.billing_software.dto.WaterMeterAssignmentRequest;

import com.ubiqedge.billing_software.dto.WaterMeterAssignmentResponse;

import com.ubiqedge.billing_software.entity.User;

import com.ubiqedge.billing_software.entity.UserSession;

import com.ubiqedge.billing_software.service.WaterMeterAssignmentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import static com.ubiqedge.billing_software.constant.AppConstant.SUCCESS;

@RestController

@RequestMapping("/api/water-meter-assignments")
public class WaterMeterAssignmentController {

    private final WaterMeterAssignmentService waterMeterAssignmentService;

    public WaterMeterAssignmentController(

            WaterMeterAssignmentService waterMeterAssignmentService) {

        this.waterMeterAssignmentService = waterMeterAssignmentService;

    }

    @PostMapping
    public ResponseEntity<ApiResponse<WaterMeterAssignmentResponse>> assign(

            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User loggedInUser,

            @Valid @RequestBody WaterMeterAssignmentRequest request) {

        WaterMeterAssignmentResponse response =

                waterMeterAssignmentService.assign(

                        userSession,

                        loggedInUser,

                        request

                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        response
                ));

    }

    @DeleteMapping

    public ResponseEntity<ApiResponse<Void>> unassign(

            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User loggedInUser,

            @Valid @RequestBody WaterMeterAssignmentRequest request) {

        waterMeterAssignmentService.unassign(

                userSession,

                loggedInUser,

                request

        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        null
                ));

    }

}
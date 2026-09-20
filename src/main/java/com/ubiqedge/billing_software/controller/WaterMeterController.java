package com.ubiqedge.billing_software.controller;

import com.ubiqedge.billing_software.dto.CreateWaterMeterRequest;
import com.ubiqedge.billing_software.dto.UpdateWaterMeterRequest;
import com.ubiqedge.billing_software.dto.WaterMeterResponse;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.dto.ApiResponse;
import com.ubiqedge.billing_software.service.WaterMeterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@RestController
@RequestMapping("/api/water-meters")
public class WaterMeterController {

    private final WaterMeterService waterMeterService;

    public WaterMeterController(
            WaterMeterService waterMeterService) {

        this.waterMeterService = waterMeterService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WaterMeterResponse>> createWaterMeter(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @RequestBody CreateWaterMeterRequest request) {

        WaterMeterResponse response =
                waterMeterService.create(
                        userSession,
                        user,
                        request
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        response
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WaterMeterResponse>> getWaterMeter(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID id) {

        WaterMeterResponse response =
                waterMeterService.getById(
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<WaterMeterResponse>>> getWaterMeters(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user) {

        List<WaterMeterResponse> response = waterMeterService.getAll(
                userSession,
                user
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
    public ResponseEntity<ApiResponse<WaterMeterResponse>> updateWaterMeter(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID id,
            @RequestBody UpdateWaterMeterRequest request) {

        WaterMeterResponse response =
                waterMeterService.update(
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
    public ResponseEntity<ApiResponse<Void>> deleteWaterMeter(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID id) {

        waterMeterService.delete(
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


/*
package com.ubiqedge.billing_software.controller;

import com.ubiqedge.billing_software.dto.WaterMeterResponse;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.dto.ApiResponse;
import com.ubiqedge.billing_software.service.CustomerMeterService;
import com.ubiqedge.billing_software.service.WaterMeterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.SUCCESS;

@RestController
@RequestMapping("/api/customer/meters")
public class CustomerMeterController {

    private final WaterMeterService watMeterService;

    public CustomerMeterController(
            WaterMeterService watMeterService) {

        this.watMeterService = watMeterService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WaterMeterResponse>>> getMyMeters(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user) {

        List<WaterMeterResponse> response =
                watMeterService.getMyMeters(
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

    @GetMapping("/{waterMeterId}")
    public ResponseEntity<ApiResponse<WaterMeterResponse>> getMyMeter(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID waterMeterId) {

        WaterMeterResponse response =
                watMeterService.getMyMeter(
                        userSession,
                        user,
                        waterMeterId
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
}

*/

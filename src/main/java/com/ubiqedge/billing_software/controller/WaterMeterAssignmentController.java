package com.ubiqedge.billing_software.controller;



import com.ubiqedge.billing_software.dto.AssignWaterMeterRequest;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.dto.ApiResponse;
import com.ubiqedge.billing_software.service.WaterMeterAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.SUCCESS;

@RestController
@RequestMapping("/api/water-meters")
public class WaterMeterAssignmentController {

    private final WaterMeterAssignmentService waterMeterAssignmentService;

    public WaterMeterAssignmentController(
            WaterMeterAssignmentService waterMeterAssignmentService) {
        this.waterMeterAssignmentService = waterMeterAssignmentService;
    }

    @PostMapping("/{waterMeterId}/assign")
    public ResponseEntity<ApiResponse<Void>> assignMeterToUser(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID waterMeterId,
            @RequestBody AssignWaterMeterRequest request) {

        waterMeterAssignmentService.assignMeterToUser(
                userSession,
                user,
                waterMeterId,
                request
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

    @DeleteMapping("/{waterMeterId}/assign")
    public ResponseEntity<ApiResponse<Void>> unassignMeterFromUser(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID waterMeterId) {

        waterMeterAssignmentService.unassignMeterFromUser(
                userSession,
                user,
                waterMeterId
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

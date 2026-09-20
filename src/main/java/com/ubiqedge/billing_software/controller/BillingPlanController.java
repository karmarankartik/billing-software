package com.ubiqedge.billing_software.controller;


import com.ubiqedge.billing_software.dto.CreateBillingPlanRequest;
import com.ubiqedge.billing_software.dto.UpdateBillingPlanRequest;
import com.ubiqedge.billing_software.dto.BillingPlanResponse;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.dto.ApiResponse;
import com.ubiqedge.billing_software.service.BillingPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

import java.util.List;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@RestController
@RequestMapping("/api/billing-plans")
public class BillingPlanController {

    private final BillingPlanService billingPlanService;

    public BillingPlanController(
            BillingPlanService billingPlanService) {

        this.billingPlanService = billingPlanService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BillingPlanResponse>> createBillingPlan(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @RequestBody CreateBillingPlanRequest request) {

        BillingPlanResponse response =
                billingPlanService.createBillingPlan(
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
    public ResponseEntity<ApiResponse<BillingPlanResponse>> getBillingPlan(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID id) {

        BillingPlanResponse response =
                billingPlanService.getBillingPlan(
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
    public ResponseEntity<ApiResponse<List<BillingPlanResponse>>> getBillingPlans(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user) {

        List<BillingPlanResponse> response =
                billingPlanService.getBillingPlans(
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
    public ResponseEntity<ApiResponse<BillingPlanResponse>> updateBillingPlan(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID id,
            @RequestBody UpdateBillingPlanRequest request) {

        BillingPlanResponse response =
                billingPlanService.updateBillingPlan(
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
    public ResponseEntity<ApiResponse<Void>> deleteBillingPlan(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID id) {

        billingPlanService.deleteBillingPlan(
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


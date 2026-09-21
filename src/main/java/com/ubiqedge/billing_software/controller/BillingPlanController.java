package com.ubiqedge.billing_software.controller;

import com.ubiqedge.billing_software.dto.ApiResponse;
import com.ubiqedge.billing_software.dto.BillingPlanResponse;
import com.ubiqedge.billing_software.dto.CreateBillingPlanRequest;
import com.ubiqedge.billing_software.dto.UpdateBillingPlanRequest;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.service.BillingPlanService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.SUCCESS;

@RestController
@RequestMapping("/api/billing-plans")
public class BillingPlanController {

    private final BillingPlanService billingPlanService;

    public BillingPlanController(
            BillingPlanService billingPlanService) {

        this.billingPlanService = billingPlanService;
    }

    /*
     * ============================================================
     * CREATE BILLING PLAN
     * ============================================================
     */

    @PostMapping
    public ResponseEntity<ApiResponse<BillingPlanResponse>> create(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @Valid @RequestBody CreateBillingPlanRequest request) {

        BillingPlanResponse response =
                billingPlanService.create(
                        userSession,
                        user,
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

    /*
     * ============================================================
     * GET BILLING PLAN BY ID
     * ============================================================
     */

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BillingPlanResponse>> getById(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable("id") UUID id) {

        BillingPlanResponse response =
                billingPlanService.getById(
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
                ));
    }

    /*
     * ============================================================
     * GET ALL BILLING PLANS
     * ============================================================
     */

    @GetMapping
    public ResponseEntity<ApiResponse<List<BillingPlanResponse>>> getAll(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user) {

        List<BillingPlanResponse> response =
                billingPlanService.getAll(
                        userSession,
                        user
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        response
                ));
    }

    /*
     * ============================================================
     * UPDATE BILLING PLAN
     * ============================================================
     */

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BillingPlanResponse>> update(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateBillingPlanRequest request) {

        BillingPlanResponse response =
                billingPlanService.update(
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
                ));
    }

    /*
     * ============================================================
     * DELETE BILLING PLAN
     * ============================================================
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable("id") UUID id) {

        billingPlanService.delete(
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

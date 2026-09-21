package com.ubiqedge.billing_software.service;

import com.ubiqedge.billing_software.dto.CreateWaterMeterRequest;
import com.ubiqedge.billing_software.dto.UpdateWaterMeterRequest;
import com.ubiqedge.billing_software.dto.WaterMeterResponse;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.entity.WaterMeter;
import com.ubiqedge.billing_software.entity.WaterMeterBillingPlan;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.BillingPlanRepository;
import com.ubiqedge.billing_software.repository.WaterMeterBillingPlanRepository;
import com.ubiqedge.billing_software.repository.WaterMeterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@Service
public class WaterMeterService {

    private final WaterMeterRepository waterMeterRepository;
    private final WaterMeterBillingPlanRepository waterMeterBillingPlanRepository;
    private final BillingPlanRepository billingPlanRepository;

    public WaterMeterService(
            WaterMeterRepository waterMeterRepository,
            WaterMeterBillingPlanRepository waterMeterBillingPlanRepository,
            BillingPlanRepository billingPlanRepository) {

        this.waterMeterRepository = waterMeterRepository;
        this.waterMeterBillingPlanRepository = waterMeterBillingPlanRepository;
        this.billingPlanRepository = billingPlanRepository;
    }

    @Transactional
    public WaterMeterResponse create(
            UserSession userSession,
            User loggedInUser,
            CreateWaterMeterRequest request) {

        validateAdmin(userSession, loggedInUser);

        if (waterMeterRepository
                .existsByMeterNumberAndDeletedAtIsNull(request.meterNumber())) {

            throw new ApiException(
                    WATER_METER_ALREADY_EXISTS,
                    HttpStatus.BAD_REQUEST
            );
        }

        /*
         * Billing plan is optional.
         *
         * If supplied, it must exist, be active
         * and must not be deleted.
         */
        if (request.billingPlanId() != null
                && !billingPlanRepository
                .existsByIdAndActiveTrueAndDeletedAtIsNull(
                        request.billingPlanId())) {

            throw new ApiException(
                    INVALID_BILLING_PLAN_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }

        Instant now = Instant.now();

        WaterMeter waterMeter = new WaterMeter();

        waterMeter.setMeterNumber(request.meterNumber());
        waterMeter.setCreatedBy(loggedInUser.getId());
        waterMeter.setCreatedAt(now);
        waterMeter.setUpdatedBy(loggedInUser.getId());
        waterMeter.setUpdatedAt(now);
        waterMeter.setDeletedAt(null);

        WaterMeter savedWaterMeter =
                waterMeterRepository.save(waterMeter);

        /*
         * Create billing-plan mapping only when
         * a billing plan was supplied.
         */
        if (request.billingPlanId() != null) {

            WaterMeterBillingPlan mapping =
                    new WaterMeterBillingPlan();

            mapping.setWaterMeterId(savedWaterMeter.getId());
            mapping.setBillingPlanId(request.billingPlanId());
            mapping.setEffectiveFrom(now);
            mapping.setEffectiveTo(null);

            /*
             * Identifies the currently active mapping.
             */
            mapping.setActivePlanKey(savedWaterMeter.getId());

            mapping.setCreatedBy(loggedInUser.getId());
            mapping.setCreatedAt(now);

            waterMeterBillingPlanRepository.save(mapping);
        }

        return toResponse(savedWaterMeter);
    }


    public List<WaterMeterResponse> getMetersForUser(
            UserSession userSession,
            User loggedInUser) {

        List<WaterMeter> waterMeters =
                waterMeterRepository.findActiveMetersByUserId(
                        loggedInUser.getId());

        return waterMeters.stream()
                .map(this::toResponse)
                .toList();
    }




    public WaterMeterResponse getById(
            UserSession userSession,
            User loggedInUser,
            UUID id) {

        validateAdmin(userSession, loggedInUser);

        WaterMeter waterMeter =
                waterMeterRepository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new ApiException(
                                WATER_METER_NOT_FOUND,
                                HttpStatus.BAD_REQUEST
                        ));

        return toResponse(waterMeter);
    }

    public List<WaterMeterResponse> getAll(
            UserSession userSession,
            User loggedInUser) {

        validateAdmin(userSession, loggedInUser);

        return waterMeterRepository
                .findAllByDeletedAtIsNull()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WaterMeterResponse update(
            UserSession userSession,
            User loggedInUser,
            UUID id,
            UpdateWaterMeterRequest request) {

        validateAdmin(userSession, loggedInUser);

        WaterMeter waterMeter =
                waterMeterRepository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new ApiException(
                                WATER_METER_NOT_FOUND,
                                HttpStatus.BAD_REQUEST
                        ));

        if (!waterMeter.getMeterNumber().equals(request.meterNumber())
                && waterMeterRepository
                .existsByMeterNumberAndDeletedAtIsNull(
                        request.meterNumber())) {

            throw new ApiException(
                    WATER_METER_ALREADY_EXISTS,
                    HttpStatus.BAD_REQUEST
            );
        }

        /*
         * If a billing plan is supplied during update,
         * validate it before modifying anything.
         */
        if (request.billingPlanId() != null
                && !billingPlanRepository
                .existsByIdAndActiveTrueAndDeletedAtIsNull(
                        request.billingPlanId())) {

            throw new ApiException(
                    INVALID_BILLING_PLAN_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }

        Instant now = Instant.now();

        waterMeter.setMeterNumber(request.meterNumber());
        waterMeter.setUpdatedBy(loggedInUser.getId());
        waterMeter.setUpdatedAt(now);

        WaterMeter updatedWaterMeter =
                waterMeterRepository.save(waterMeter);

        /*
         * If billingPlanId is supplied:
         *
         * 1. Close the current active mapping.
         * 2. Create a new active mapping.
         *
         * If billingPlanId is null:
         * Leave the existing billing-plan mapping unchanged.
         */
        if (request.billingPlanId() != null) {

            waterMeterBillingPlanRepository
                    .findByWaterMeterIdAndEffectiveToIsNull(id)
                    .ifPresent(existingMapping -> {

                        existingMapping.setEffectiveTo(now);
                        existingMapping.setActivePlanKey(null);

                        waterMeterBillingPlanRepository.save(
                                existingMapping
                        );
                    });

            WaterMeterBillingPlan newMapping =
                    new WaterMeterBillingPlan();

            newMapping.setWaterMeterId(id);
            newMapping.setBillingPlanId(request.billingPlanId());
            newMapping.setEffectiveFrom(now);
            newMapping.setEffectiveTo(null);
            newMapping.setActivePlanKey(id);
            newMapping.setCreatedBy(loggedInUser.getId());
            newMapping.setCreatedAt(now);

            waterMeterBillingPlanRepository.save(newMapping);
        }

        return toResponse(updatedWaterMeter);
    }

    @Transactional
    public void delete(
            UserSession userSession,
            User loggedInUser,
            UUID id) {

        validateAdmin(userSession, loggedInUser);

        WaterMeter waterMeter =
                waterMeterRepository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new ApiException(
                                WATER_METER_NOT_FOUND,
                                HttpStatus.BAD_REQUEST
                        ));

        Instant now = Instant.now();

        waterMeter.setDeletedAt(now);
        waterMeter.setUpdatedAt(now);
        waterMeter.setUpdatedBy(loggedInUser.getId());

        waterMeterRepository.save(waterMeter);
    }

    private void validateAdmin(
            UserSession userSession,
            User loggedInUser) {

        if (userSession == null || loggedInUser == null) {

            throw new ApiException(
                    INVALID_SESSION,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!ROLE_ADMIN.equals(loggedInUser.getRole())) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED.toString(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }


    private WaterMeterResponse toResponse(WaterMeter waterMeter) {

        UUID billingPlanId = waterMeterBillingPlanRepository
                .findByWaterMeterIdAndEffectiveToIsNull(waterMeter.getId())
                .map(WaterMeterBillingPlan::getBillingPlanId)
                .orElse(null);

        return new WaterMeterResponse(
                waterMeter.getId(),
                waterMeter.getMeterNumber(),
                billingPlanId,
                waterMeter.getCreatedBy(),
                waterMeter.getCreatedAt(),
                waterMeter.getUpdatedBy(),
                waterMeter.getUpdatedAt()
        );

    }


}

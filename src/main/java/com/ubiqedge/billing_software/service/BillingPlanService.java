package com.ubiqedge.billing_software.service;

import com.ubiqedge.billing_software.dto.BillingPlanResponse;
import com.ubiqedge.billing_software.dto.BillingPlanSlabResponse;
import com.ubiqedge.billing_software.dto.BillingSlabRequest;
import com.ubiqedge.billing_software.dto.CreateBillingPlanRequest;
import com.ubiqedge.billing_software.dto.UpdateBillingPlanRequest;
import com.ubiqedge.billing_software.entity.BillingPlan;
import com.ubiqedge.billing_software.entity.BillingPlanSlab;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.BillingPlanRepository;
import com.ubiqedge.billing_software.repository.BillingPlanSlabRepository;
import com.ubiqedge.billing_software.entity.UserSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@Service
public class BillingPlanService {



    private final BillingPlanRepository billingPlanRepository;
    private final BillingPlanSlabRepository billingPlanSlabRepository;

    public BillingPlanService(
            BillingPlanRepository billingPlanRepository,
            BillingPlanSlabRepository billingPlanSlabRepository) {

        this.billingPlanRepository = billingPlanRepository;
        this.billingPlanSlabRepository = billingPlanSlabRepository;
    }

    /*
     * ============================================================
     * CREATE
     * ============================================================
     */

    @Transactional
    public BillingPlanResponse create(
            UserSession userSession,
            User loggedInUser,
            CreateBillingPlanRequest request) {

        validateAdmin(userSession, loggedInUser);

        validateCreateRequest(request);

        if (billingPlanRepository
                .existsByCodeAndDeletedAtIsNull(request.code())) {

            throw new ApiException(
                    BILLING_PLAN_ALREADY_EXISTS,
                    HttpStatus.BAD_REQUEST
            );
        }

        Instant now = Instant.now();

        BillingPlan billingPlan = new BillingPlan();

        billingPlan.setName(request.name());
        billingPlan.setCode(request.code());
        billingPlan.setDescription(request.description());
        billingPlan.setPlanType(request.planType());
        billingPlan.setPricePerUnit(request.pricePerUnit());
        billingPlan.setActive(true);

        billingPlan.setCreatedBy(loggedInUser.getId());
        billingPlan.setCreatedAt(now);
        billingPlan.setUpdatedBy(loggedInUser.getId());
        billingPlan.setUpdatedAt(now);
        billingPlan.setDeletedAt(null);

        BillingPlan savedPlan =
                billingPlanRepository.save(billingPlan);

        /*
         * Slabs are created only for SLAB plans.
         */
        if (SLAB.equals(request.planType())) {

            createSlabs(
                    savedPlan.getId(),
                    request.slabs(),
                    now
            );
        }

        return toResponse(savedPlan);
    }

    /*
     * ============================================================
     * GET BY ID
     * ============================================================
     */

    @Transactional(readOnly = true)
    public BillingPlanResponse getById(
            UserSession userSession,
            User loggedInUser,
            UUID billingPlanId) {

        validateAdmin(userSession, loggedInUser);

        BillingPlan billingPlan =
                billingPlanRepository
                        .findByIdAndDeletedAtIsNull(billingPlanId)
                        .orElseThrow(() ->
                                new ApiException(
                                        BILLING_PLAN_NOT_FOUND,
                                        HttpStatus.NOT_FOUND
                                )
                        );

        return toResponse(billingPlan);
    }

    /*
     * ============================================================
     * GET ALL
     * ============================================================
     */

    @Transactional(readOnly = true)
    public List<BillingPlanResponse> getAll(
            UserSession userSession,
            User loggedInUser) {

        validateAdmin(userSession, loggedInUser);

        return billingPlanRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * ============================================================
     * UPDATE
     * ============================================================
     */

    @Transactional
    public BillingPlanResponse update(
            UserSession userSession,
            User loggedInUser,
            UUID billingPlanId,
            UpdateBillingPlanRequest request) {

        validateAdmin(userSession, loggedInUser);

        validateUpdateRequest(request);

        BillingPlan billingPlan =
                billingPlanRepository
                        .findByIdAndDeletedAtIsNull(billingPlanId)
                        .orElseThrow(() ->
                                new ApiException(
                                        BILLING_PLAN_NOT_FOUND,
                                        HttpStatus.NOT_FOUND
                                )
                        );

        Instant now = Instant.now();

        /*
         * Update common plan information.
         */
        billingPlan.setName(request.name());
        billingPlan.setDescription(request.description());
        billingPlan.setPlanType(request.planType());
        billingPlan.setPricePerUnit(request.pricePerUnit());
        billingPlan.setUpdatedBy(loggedInUser.getId());
        billingPlan.setUpdatedAt(now);

        /*
         * ========================================================
         * FIXED
         * ========================================================
         *
         * Any existing active slabs are no longer applicable.
         */
        if (FIXED.equals(request.planType())) {

            softDeleteActiveSlabs(
                    billingPlanId,
                    now
            );
        }

        /*
         * ========================================================
         * SLAB
         * ========================================================
         *
         * Replace the active slab configuration.
         */
        if (SLAB.equals(request.planType())) {

            softDeleteActiveSlabs(
                    billingPlanId,
                    now
            );

            createSlabs(
                    billingPlanId,
                    request.slabs(),
                    now
            );
        }

        BillingPlan savedPlan =
                billingPlanRepository.save(billingPlan);

        return toResponse(savedPlan);
    }

    /*
     * ============================================================
     * DELETE
     * ============================================================
     *
     * Soft delete.
     */

    @Transactional
    public void delete(
            UserSession userSession,
            User loggedInUser,
            UUID billingPlanId) {

        validateAdmin(userSession, loggedInUser);

        BillingPlan billingPlan =
                billingPlanRepository
                        .findByIdAndDeletedAtIsNull(billingPlanId)
                        .orElseThrow(() ->
                                new ApiException(
                                        BILLING_PLAN_NOT_FOUND,
                                        HttpStatus.NOT_FOUND
                                )
                        );

        Instant now = Instant.now();

        billingPlan.setActive(false);
        billingPlan.setDeletedAt(now);
        billingPlan.setUpdatedBy(loggedInUser.getId());
        billingPlan.setUpdatedAt(now);

        /*
         * Slabs belong to this plan, so soft-delete their
         * active records as well.
         */
        softDeleteActiveSlabs(
                billingPlanId,
                now
        );

        billingPlanRepository.save(billingPlan);
    }

    /*
     * ============================================================
     * VALIDATE CREATE
     * ============================================================
     */

    private void validateCreateRequest(
            CreateBillingPlanRequest request) {

        validateCommonFields(
                request.name(),
                request.code(),
                request.planType()
        );

        validatePlanConfiguration(
                request.planType(),
                request.pricePerUnit(),
                request.slabs()
        );
    }

    /*
     * ============================================================
     * VALIDATE UPDATE
     * ============================================================
     */

    private void validateUpdateRequest(
            UpdateBillingPlanRequest request) {

        validateCommonFields(
                request.name(),
                null,
                request.planType()
        );

        validatePlanConfiguration(
                request.planType(),
                request.pricePerUnit(),
                request.slabs()
        );
    }

    /*
     * ============================================================
     * COMMON VALIDATION
     * ============================================================
     */

    private void validateCommonFields(
            String name,
            String code,
            String planType) {

        if (name == null || name.isBlank()) {
            throw new ApiException(
                    INVALID_BILLING_PLAN_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (code != null && code.isBlank()) {
            throw new ApiException(
                    INVALID_BILLING_PLAN_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (planType == null || planType.isBlank()) {
            throw new ApiException(
                    INVALID_BILLING_PLAN_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!FIXED.equals(planType)
                && !SLAB.equals(planType)) {

            throw new ApiException(
                    INVALID_BILLING_PLAN_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    /*
     * ============================================================
     * PLAN TYPE VALIDATION
     * ============================================================
     */

    private void validatePlanConfiguration(
            String planType,
            BigDecimal pricePerUnit,
            List<BillingSlabRequest> slabs) {

        if (FIXED.equals(planType)) {

            /*
             * FIXED requires price.
             */
            if (pricePerUnit == null
                    || pricePerUnit.signum() < 0) {

                throw new ApiException(
                        INVALID_BILLING_PLAN_REQUEST,
                        HttpStatus.BAD_REQUEST
                );
            }

            /*
             * FIXED must not contain slabs.
             */
            if (slabs != null && !slabs.isEmpty()) {

                throw new ApiException(
                        INVALID_BILLING_PLAN_REQUEST,
                        HttpStatus.BAD_REQUEST
                );
            }

            return;
        }

        /*
         * SLAB must not have plan-level price.
         */
        if (pricePerUnit != null) {

            throw new ApiException(
                    INVALID_BILLING_PLAN_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }

        /*
         * SLAB requires at least one slab.
         */
        if (slabs == null || slabs.isEmpty()) {

            throw new ApiException(
                    INVALID_BILLING_PLAN_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }

        validateSlabs(slabs);
    }

    /*
     * ============================================================
     * SLAB VALIDATION
     * ============================================================
     */

    private void validateSlabs(
            List<BillingSlabRequest> slabs) {

        for (BillingSlabRequest slab : slabs) {

            if (slab.lowerBound() == null) {

                throw new ApiException(
                        INVALID_BILLING_SLAB,
                        HttpStatus.BAD_REQUEST
                );
            }

            if (slab.lowerBound().signum() < 0) {

                throw new ApiException(
                        INVALID_BILLING_SLAB,
                        HttpStatus.BAD_REQUEST
                );
            }

            if (slab.pricePerUnit() == null
                    || slab.pricePerUnit().signum() < 0) {

                throw new ApiException(
                        INVALID_BILLING_SLAB,
                        HttpStatus.BAD_REQUEST
                );
            }

            if (slab.upperBound() != null
                    && slab.upperBound()
                    .compareTo(slab.lowerBound()) <= 0) {

                throw new ApiException(
                        INVALID_BILLING_SLAB,
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        /*
         * Sort only a copy so the request object itself
         * is not modified.
         */
        List<BillingSlabRequest> sortedSlabs =
                slabs.stream()
                        .sorted(Comparator.comparing(
                                BillingSlabRequest::lowerBound))
                        .toList();

        /*
         * First slab must start at zero.
         */
        if (sortedSlabs.get(0)
                .lowerBound()
                .compareTo(BigDecimal.ZERO) != 0) {

            throw new ApiException(
                    INVALID_BILLING_SLAB,
                    HttpStatus.BAD_REQUEST
            );
        }

        /*
         * Validate continuous, non-overlapping ranges.
         *
         * Example:
         *
         * 0 - 10
         * 10 - 20
         * 20 - null
         *
         * Valid.
         */
        for (int i = 0; i < sortedSlabs.size() - 1; i++) {

            BillingSlabRequest current =
                    sortedSlabs.get(i);

            BillingSlabRequest next =
                    sortedSlabs.get(i + 1);

            /*
             * An open-ended slab must be the last slab.
             */
            if (current.upperBound() == null) {

                throw new ApiException(
                        INVALID_BILLING_SLAB,
                        HttpStatus.BAD_REQUEST
                );
            }

            /*
             * Next lower bound must exactly equal
             * current upper bound.
             */
            if (current.upperBound()
                    .compareTo(next.lowerBound()) != 0) {

                throw new ApiException(
                        INVALID_BILLING_SLAB,
                        HttpStatus.BAD_REQUEST
                );
            }
        }
    }

    /*
     * ============================================================
     * CREATE SLABS
     * ============================================================
     */

    private void createSlabs(
            UUID billingPlanId,
            List<BillingSlabRequest> slabs,
            Instant now) {

        OffsetDateTime timestamp =
                now.atOffset((ZoneOffset) BILLING_ZONE);

        for (BillingSlabRequest request : slabs) {

            BillingPlanSlab slab =
                    new BillingPlanSlab();

            slab.setBillingPlanId(billingPlanId);
            slab.setLowerBound(request.lowerBound());
            slab.setUpperBound(request.upperBound());
            slab.setPricePerUnit(request.pricePerUnit());
            slab.setCreatedAt(timestamp.toInstant());
            slab.setUpdatedAt(timestamp.toInstant());
            slab.setDeletedAt(null);

            billingPlanSlabRepository.save(slab);
        }
    }

    /*
     * ============================================================
     * SOFT DELETE ACTIVE SLABS
     * ============================================================
     */

    private void softDeleteActiveSlabs(
            UUID billingPlanId,
            Instant now) {

        OffsetDateTime timestamp =
                now.atOffset((ZoneOffset) BILLING_ZONE);

        List<BillingPlanSlab> slabs =
                billingPlanSlabRepository
                        .findAllByBillingPlanIdAndDeletedAtIsNull(
                                billingPlanId
                        );

        for (BillingPlanSlab slab : slabs) {

            slab.setDeletedAt(timestamp.toInstant());
            slab.setUpdatedAt(timestamp.toInstant());
        }

        if (!slabs.isEmpty()) {
            billingPlanSlabRepository.saveAll(slabs);
        }
    }

    /*
     * ============================================================
     * RESPONSE MAPPING
     * ============================================================
     */

    private BillingPlanResponse toResponse(
            BillingPlan billingPlan) {

        List<BillingPlanSlabResponse> slabs =
                billingPlanSlabRepository
                        .findAllByBillingPlanIdAndDeletedAtIsNull(
                                billingPlan.getId()
                        )
                        .stream()
                        .map(this::toSlabResponse)
                        .toList();

        return new BillingPlanResponse(
                billingPlan.getId(),
                billingPlan.getName(),
                billingPlan.getCode(),
                billingPlan.getDescription(),
                billingPlan.getPlanType(),
                billingPlan.getPricePerUnit(),
                billingPlan.isActive(),
                billingPlan.getCreatedBy(),
                billingPlan.getCreatedAt(),
                billingPlan.getUpdatedBy(),
                billingPlan.getUpdatedAt(),
                slabs
        );
    }

    private BillingPlanSlabResponse toSlabResponse(
            BillingPlanSlab slab) {

        return new BillingPlanSlabResponse(
                slab.getId(),
                slab.getLowerBound(),
                slab.getUpperBound(),
                slab.getPricePerUnit(),
                slab.getCreatedAt(),
                slab.getUpdatedAt()
        );
    }

    /*
     * ============================================================
     * ADMIN VALIDATION
     * ============================================================
     */

    private void validateAdmin(
            UserSession userSession,
            User loggedInUser) {

        /*
         * Use the same implementation as your
         * UserService / WaterMeterService.
         *
         * Keep the actual authorization logic centralized
         * if your existing service already has a common method.
         */
        if (loggedInUser == null
                || loggedInUser.getRole() == null
                || !ROLE_ADMIN.equals(loggedInUser.getRole())) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED.toString(),
                    HttpStatus.BAD_REQUEST

            );
        }
    }
}


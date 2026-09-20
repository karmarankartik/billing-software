package com.ubiqedge.billing_software.service;




import com.ubiqedge.billing_software.dto.BillingPlanResponse;
import com.ubiqedge.billing_software.dto.CreateBillingPlanRequest;
import com.ubiqedge.billing_software.dto.UpdateBillingPlanRequest;
import com.ubiqedge.billing_software.entity.BillingPlan;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.BillingPlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@Service
public class BillingPlanService {

    private final BillingPlanRepository billingPlanRepository;

    public BillingPlanService(
            BillingPlanRepository billingPlanRepository) {

        this.billingPlanRepository = billingPlanRepository;
    }

    @Transactional
    public BillingPlanResponse createBillingPlan(
            UserSession userSession,
            User user,
            CreateBillingPlanRequest request) {

        validateAdmin(userSession, user);
        validateCreateRequest(request);

        if (billingPlanRepository.existsByCode(request.code().trim())) {
            throw new ApiException(
                    BILLING_PLAN_ALREADY_EXISTS,
                    HttpStatus.BAD_REQUEST
            );
        }

        Instant now = Instant.now();

        BillingPlan billingPlan = new BillingPlan();

        billingPlan.setName(request.name().trim());
        billingPlan.setCode(request.code().trim());
        billingPlan.setDescription(
                request.description() == null
                        ? null
                        : request.description().trim()
        );
        billingPlan.setPricePerUnit(request.pricePerUnit());
        billingPlan.setActive(true);

        billingPlan.setCreatedBy(user.getId());
        billingPlan.setCreatedAt(now);
        billingPlan.setUpdatedBy(user.getId());
        billingPlan.setUpdatedAt(now);

        BillingPlan savedBillingPlan =
                billingPlanRepository.save(billingPlan);

        return toResponse(savedBillingPlan);
    }

    @Transactional(readOnly = true)
    public BillingPlanResponse getBillingPlan(
            UserSession userSession,
            User user,
            UUID id) {

        validateAdmin(userSession, user);

        BillingPlan billingPlan =
                getActiveBillingPlan(id);

        return toResponse(billingPlan);
    }

    @Transactional(readOnly = true)
    public List<BillingPlanResponse> getBillingPlans(
            UserSession userSession,
            User user) {

        validateAdmin(userSession, user);

        return billingPlanRepository.findAllActive()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BillingPlanResponse updateBillingPlan(
            UserSession userSession,
            User user,
            UUID id,
            UpdateBillingPlanRequest request) {

        validateAdmin(userSession, user);
        validateUpdateRequest(request);

        BillingPlan billingPlan =
                getActiveBillingPlan(id);

        String code = request.code().trim();

        if (billingPlanRepository.existsByCodeAndIdNot(
                code,
                id)) {

            throw new ApiException(
                    BILLING_PLAN_ALREADY_EXISTS,
                    HttpStatus.CONFLICT
            );
        }

        Instant now = Instant.now();

        billingPlan.setName(request.name().trim());
        billingPlan.setCode(code);
        billingPlan.setDescription(
                request.description() == null
                        ? null
                        : request.description().trim()
        );
        billingPlan.setPricePerUnit(
                request.pricePerUnit()
        );
        billingPlan.setActive(
                request.active()
        );

        billingPlan.setUpdatedBy(user.getId());
        billingPlan.setUpdatedAt(now);

        BillingPlan savedBillingPlan =
                billingPlanRepository.save(billingPlan);

        return toResponse(savedBillingPlan);
    }

    @Transactional
    public void deleteBillingPlan(
            UserSession userSession,
            User user,
            UUID id) {

        validateAdmin(userSession, user);

        BillingPlan billingPlan =
                getActiveBillingPlan(id);

        Instant now = Instant.now();

        billingPlan.setActive(false);
        billingPlan.setDeletedAt(now);
        billingPlan.setUpdatedBy(user.getId());
        billingPlan.setUpdatedAt(now);

        billingPlanRepository.save(billingPlan);
    }

    private BillingPlan getActiveBillingPlan(UUID id) {

        return billingPlanRepository
                .findActiveById(id)
                .orElseThrow(() ->
                        new ApiException(
                                BILLING_PLAN_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );
    }

    private void validateCreateRequest(
            CreateBillingPlanRequest request) {

        if (request == null
                || request.name() == null
                || request.name().isBlank()
                || request.code() == null
                || request.code().isBlank()
                || request.pricePerUnit() == null
                || request.pricePerUnit()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new ApiException(
                    INVALID_BILLING_PLAN_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateUpdateRequest(
            UpdateBillingPlanRequest request) {

        if (request == null
                || request.name() == null
                || request.name().isBlank()
                || request.code() == null
                || request.code().isBlank()
                || request.pricePerUnit() == null
                || request.pricePerUnit()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new ApiException(
                    INVALID_BILLING_PLAN_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateAdmin(
            UserSession userSession,
            User user) {

        if (userSession == null || user == null) {

            throw new ApiException(
                    INVALID_SESSION,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!"ADMIN".equals(user.getRole())) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED.toString(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private BillingPlanResponse toResponse(
            BillingPlan billingPlan) {

        return new BillingPlanResponse(
                billingPlan.getId(),
                billingPlan.getName(),
                billingPlan.getCode(),
                billingPlan.getDescription(),
                billingPlan.getPricePerUnit(),
                billingPlan.isActive(),
                billingPlan.getCreatedAt(),
                billingPlan.getUpdatedAt()
        );
    }
}


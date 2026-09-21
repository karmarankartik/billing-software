package com.ubiqedge.billing_software.service;


import com.ubiqedge.billing_software.dto.WaterMeterAssignmentRequest;
import com.ubiqedge.billing_software.dto.WaterMeterAssignmentResponse;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.WaterMeter;
import com.ubiqedge.billing_software.entity.WaterMeterAssignment;
import com.ubiqedge.billing_software.entity.WaterMeterBillingPlan;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.UserRepository;
import com.ubiqedge.billing_software.repository.WaterMeterAssignmentRepository;
import com.ubiqedge.billing_software.repository.WaterMeterBillingPlanRepository;
import com.ubiqedge.billing_software.repository.WaterMeterRepository;
import com.ubiqedge.billing_software.entity.UserSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import static com.ubiqedge.billing_software.constant.AppConstant.*;




@Service public class WaterMeterAssignmentService {


    private final WaterMeterAssignmentRepository waterMeterAssignmentRepository;
    private final WaterMeterRepository waterMeterRepository;
    private final UserRepository userRepository;
    private final WaterMeterBillingPlanRepository waterMeterBillingPlanRepository;

    public WaterMeterAssignmentService(WaterMeterAssignmentRepository waterMeterAssignmentRepository, WaterMeterRepository waterMeterRepository, UserRepository userRepository, WaterMeterBillingPlanRepository waterMeterBillingPlanRepository) {
        this.waterMeterAssignmentRepository = waterMeterAssignmentRepository;
        this.waterMeterRepository = waterMeterRepository;
        this.userRepository = userRepository;
        this.waterMeterBillingPlanRepository = waterMeterBillingPlanRepository;
    }


    @Transactional
    public WaterMeterAssignmentResponse assign(UserSession userSession, User loggedInUser, WaterMeterAssignmentRequest request) {
        validateAdmin(userSession, loggedInUser);
        User user = getUser(request.userId());
        WaterMeter waterMeter = getWaterMeter(request.waterMeterId());
        validateBillingPlan(waterMeter.getId());
        WaterMeterAssignment existingAssignment = waterMeterAssignmentRepository.findByWaterMeterIdAndUnassignedAtIsNull(waterMeter.getId()).orElse(null);
        if (existingAssignment != null) {
            throw new ApiException(WATER_METER_ALREADY_ASSIGNED, HttpStatus.BAD_REQUEST);
        }
        Instant now = Instant.now();
        WaterMeterAssignment assignment = new WaterMeterAssignment();
        assignment.setWaterMeterId(waterMeter.getId());
        assignment.setUserId(user.getId());
        assignment.setAssignedAt(now); /* * Active assignment: * * active_assignment_key = water_meter_id * * The database UNIQUE constraint therefore guarantees * that only one active assignment can exist for a meter. */
        assignment.setActiveAssignmentKey(waterMeter.getId());
        assignment.setCreatedBy(loggedInUser.getId());
        assignment.setCreatedAt(now);
        WaterMeterAssignment saved = waterMeterAssignmentRepository.save(assignment);
        return toResponse(saved);
    }
    @Transactional public void unassign(UserSession userSession, User loggedInUser, WaterMeterAssignmentRequest request) {
        validateAdmin(userSession, loggedInUser);
        User user = getUser(request.userId());
        WaterMeter waterMeter = getWaterMeter(request.waterMeterId());
        WaterMeterAssignment assignment = waterMeterAssignmentRepository.findByWaterMeterIdAndUnassignedAtIsNull(waterMeter.getId()).orElseThrow(() -> new ApiException(WATER_METER_NOT_ASSIGNED, HttpStatus.NOT_FOUND)); /* * The supplied user must be the user currently assigned * to this meter. */
        if (!assignment.getUserId().equals(user.getId())) {
            throw new ApiException(WATER_METER_ASSIGNMENT_USER_MISMATCH, HttpStatus.BAD_REQUEST);
        }
        assignment.setUnassignedAt(Instant.now()); /* * Historical assignment must not retain the active key. */
        assignment.setActiveAssignmentKey(null);
        waterMeterAssignmentRepository.save(assignment);
    }
    private User getUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(() -> new ApiException(USER_NOT_FOUND, HttpStatus.BAD_REQUEST));
    }
    private WaterMeter getWaterMeter(UUID waterMeterId) {
        return waterMeterRepository.findByIdAndDeletedAtIsNull(waterMeterId).orElseThrow(() -> new ApiException(WATER_METER_NOT_FOUND, HttpStatus.BAD_REQUEST));
    }
    private void validateBillingPlan(UUID waterMeterId) {
        WaterMeterBillingPlan billingPlan = waterMeterBillingPlanRepository.findByWaterMeterIdAndEffectiveToIsNull(waterMeterId).orElseThrow(() -> new ApiException(WATER_METER_BILLING_PLAN_REQUIRED, HttpStatus.BAD_REQUEST)); /* * The active association must point to an existing * billing plan. The association itself is sufficient * here because the assignment requirement is that the * meter has a billing plan associated with it. */
        if (billingPlan.getBillingPlanId() == null) {
            throw new ApiException(WATER_METER_BILLING_PLAN_REQUIRED, HttpStatus.BAD_REQUEST);
        }
    }
    private WaterMeterAssignmentResponse toResponse(WaterMeterAssignment assignment) {
        return new WaterMeterAssignmentResponse(assignment.getId(), assignment.getWaterMeterId(), assignment.getUserId(), assignment.getAssignedAt(), assignment.getUnassignedAt(), assignment.getCreatedBy(), assignment.getCreatedAt());
    }
    private void validateAdmin(UserSession userSession, User loggedInUser) {
        /* * Keep this authorization logic consistent with the * existing UserService / WaterMeterService implementation. */
        if (loggedInUser == null || loggedInUser.getRole() == null || !"ADMIN".equals(loggedInUser.getRole().toString())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.toString(), HttpStatus.BAD_REQUEST);
        }
    }
}
/*
package com.ubiqedge.billing_software.service;



import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.entity.WaterMeter;
import com.ubiqedge.billing_software.entity.WaterMeterAssignment;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.UserRepository;
import com.ubiqedge.billing_software.repository.WaterMeterAssignmentRepository;
import com.ubiqedge.billing_software.repository.WaterMeterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@Service
public class WaterMeterAssignmentService {

    private final WaterMeterRepository waterMeterRepository;
    private final UserRepository userRepository;
    private final WaterMeterAssignmentRepository waterMeterAssignmentRepository;

    public WaterMeterAssignmentService(
            WaterMeterRepository waterMeterRepository,
            UserRepository userRepository,
            WaterMeterAssignmentRepository waterMeterAssignmentRepository) {

        this.waterMeterRepository = waterMeterRepository;
        this.userRepository = userRepository;
        this.waterMeterAssignmentRepository = waterMeterAssignmentRepository;
    }

    @Transactional
    public void assignMeterToUser(
            UserSession userSession,
            User user,
            UUID waterMeterId,
            UUID userId) {

        validateAdmin(userSession, user);

        WaterMeter waterMeter = waterMeterRepository.findActiveById(waterMeterId)
                .orElseThrow(() -> new ApiException(
                        WATER_METER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        User customer = userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new ApiException(
                        USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (!ROLE_USER.equals(customer.getRole())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED.toString(),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (waterMeterAssignmentRepository
                .findActiveByWaterMeterId(waterMeter.getId())
                .isPresent()) {

            throw new ApiException(
                    WATER_METER_ALREADY_EXISTS,
                    HttpStatus.BAD_REQUEST
            );
        }

        Instant now = Instant.now();

        WaterMeterAssignment assignment = new WaterMeterAssignment();

        assignment.setWaterMeterId(waterMeter.getId());
        assignment.setUserId(customer.getId());
        assignment.setAssignedAt(now);
        assignment.setUnassignedAt(null);
        assignment.setActiveAssignmentKey(waterMeter.getId());
        assignment.setCreatedBy(user.getId());
        assignment.setCreatedAt(now);

        waterMeterAssignmentRepository.save(assignment);
    }

    @Transactional
    public void unassignMeterFromUser(
            UserSession userSession,
            User user,
            UUID waterMeterId) {

        validateAdmin(userSession, user);

        waterMeterRepository.findActiveById(waterMeterId)
                .orElseThrow(() -> new ApiException(
                        WATER_METER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        Instant now = Instant.now();

        int updatedRows =
                waterMeterAssignmentRepository.unassignActiveMeter(
                        waterMeterId,
                        now
                );

        if (updatedRows == 0) {
            throw new ApiException(
                    WATER_METER_NOT_ASSIGNED,
                    HttpStatus.NOT_FOUND
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

        if (!ROLE_ADMIN.equals(user.getRole())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED.toString(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
*/

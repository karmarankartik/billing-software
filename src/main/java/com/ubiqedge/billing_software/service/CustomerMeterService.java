/*
package com.ubiqedge.billing_software.service;



import com.ubiqedge.billing_software.dto.WaterMeterResponse;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.CustomerMeterProjection;
import com.ubiqedge.billing_software.repository.CustomerMeterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.WATER_METER_NOT_FOUND;

@Service
public class CustomerMeterService {

    private final CustomerMeterRepository customerMeterRepository;

    public CustomerMeterService(
            CustomerMeterRepository customerMeterRepository) {

        this.customerMeterRepository = customerMeterRepository;
    }

    @Transactional(readOnly = true)
    public List<WaterMeterResponse> getMyMeters(
            UserSession userSession,
            User user) {

        return customerMeterRepository
                .findActiveMetersByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WaterMeterResponse getMyMeter(
            UserSession userSession,
            User user,
            UUID waterMeterId) {

        CustomerMeterProjection meter =
                customerMeterRepository.findActiveMeterByUserId(
                        waterMeterId,
                        user.getId()
                ).orElseThrow(() -> new ApiException(
                        WATER_METER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        return toResponse(meter);
    }

    private WaterMeterResponse toResponse(
            CustomerMeterProjection meter) {

        return new WaterMeterResponse(
                meter.getId(),
                meter.getMeterNumber(),
                meter.getBillingPlanId(),
                meter.getCreatedAt(),
                waterMeter.getUpdatedBy(), meter.getUpdatedAt()
        );
    }
}
*/

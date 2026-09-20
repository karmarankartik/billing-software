package com.ubiqedge.billing_software.service;



import com.ubiqedge.billing_software.dto.CreateWaterMeterReadingRequest;
import com.ubiqedge.billing_software.entity.WaterMeter;
import com.ubiqedge.billing_software.entity.WaterMeterReading;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.WaterMeterReadingRepository;
import com.ubiqedge.billing_software.repository.WaterMeterRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@Service
public class WaterMeterReadingService {

    private final WaterMeterRepository waterMeterRepository;
    private final WaterMeterReadingRepository waterMeterReadingRepository;

    public WaterMeterReadingService(
            WaterMeterRepository waterMeterRepository,
            WaterMeterReadingRepository waterMeterReadingRepository) {

        this.waterMeterRepository = waterMeterRepository;
        this.waterMeterReadingRepository = waterMeterReadingRepository;
    }

    @Transactional
    public void ingestReading(
            UUID waterMeterId,
            CreateWaterMeterReadingRequest request) {

        validateRequest(request);

        WaterMeter waterMeter =
                waterMeterRepository.findActiveById(waterMeterId)
                        .orElseThrow(() -> new ApiException(
                                WATER_METER_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        ));

        validateReadingValue(request.readingValue());

        Instant readingAt = request.readingAt() == null
                ? Instant.now()
                : request.readingAt();

        if (waterMeterReadingRepository
                .existsByWaterMeterIdAndReadingTypeAndReadingAt(
                        waterMeter.getId(),
                        request.readingType(),
                        readingAt)) {

            throw new ApiException(
                    WATER_METER_READING_ALREADY_EXISTS ,
                    HttpStatus.CONFLICT
            );
        }

        validateReadingSequence(
                waterMeter.getId(),
                request,
                readingAt
        );

        WaterMeterReading reading = new WaterMeterReading();

        reading.setWaterMeterId(waterMeter.getId());
        reading.setReadingType(request.readingType());
        reading.setReadingValue(request.readingValue());
        reading.setReadingAt(readingAt);
        reading.setCreatedAt(Instant.now());

        try {
            waterMeterReadingRepository.saveAndFlush(reading);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(
                    WATER_METER_READING_ALREADY_EXISTS,
                    HttpStatus.CONFLICT
            );
        }
    }

    private void validateRequest(
            CreateWaterMeterReadingRequest request) {

        if (request == null
                || request.readingType() == null
                || request.readingValue() == null) {

            throw new ApiException(
                    INVALID_WATER_METER_READING_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateReadingValue(BigDecimal readingValue) {

        if (readingValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(
                    INVALID_WATER_METER_READING_REQUEST,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateReadingSequence(
            UUID waterMeterId,
            CreateWaterMeterReadingRequest request,
            Instant readingAt) {

        WaterMeterReading latestReading =
                waterMeterReadingRepository.findLatestReading(
                        waterMeterId,
                        request.readingType(),
                        readingAt
                ).orElse(null);

        if (latestReading == null) {
            return;
        }

        if (request.readingType().name().equals("TOTAL")
                && request.readingValue()
                .compareTo(latestReading.getReadingValue()) < 0) {

            throw new ApiException(
                    INVALID_TOTAL_READING,
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}


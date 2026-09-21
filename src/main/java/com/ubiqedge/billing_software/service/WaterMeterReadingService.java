package com.ubiqedge.billing_software.service;



import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.WaterMeterReading;
import com.ubiqedge.billing_software.dto.WaterMeterReadingRequest;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.WaterMeterReadingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import  static com.ubiqedge.billing_software.constant.AppConstant.*;
import java.time.Instant;

@Service
public class WaterMeterReadingService {

    private final WaterMeterReadingRepository waterMeterReadingRepository;

    public WaterMeterReadingService(
            WaterMeterReadingRepository waterMeterReadingRepository) {
        this.waterMeterReadingRepository = waterMeterReadingRepository;
    }

    @Transactional
    public void ingest(
            WaterMeterReadingRequest request) {

        validateRequest(request);

        WaterMeterReading reading = new WaterMeterReading();

        reading.setWaterMeterId(request.waterMeterId());
        reading.setReadingType(request.readingType());
        reading.setReadingValue(request.readingValue());
        reading.setReadingAt(request.readingAt());
        reading.setIngestionKey(request.ingestionKey());
        reading.setCreatedAt(Instant.now());

        waterMeterReadingRepository.save(reading);
    }

    private void validateRequest(WaterMeterReadingRequest request) {

        if (request == null) {
            throw new ApiException(
                    WATER_METER_READING_REQUEST_REQUIRED,
                    HttpStatus.BAD_REQUEST);
        }

        if (request.waterMeterId() == null) {
            throw new ApiException(
                    WATER_METER_ID_REQUIRED,
                    HttpStatus.BAD_REQUEST);
        }

        if (request.readingType() == null
                || request.readingType().isBlank()) {
            throw new ApiException(
                    WATER_METER_READING_TYPE_REQUIRED,
                    HttpStatus.BAD_REQUEST);
        }

        if (!"TOTAL".equals(request.readingType())
                && !"FLOW".equals(request.readingType())) {
            throw new ApiException(
                    INVALID_WATER_METER_READING_TYPE,
                    HttpStatus.BAD_REQUEST);
        }

        if (request.readingValue() == null) {
            throw new ApiException(
                    WATER_METER_READING_VALUE_REQUIRED,
                    HttpStatus.BAD_REQUEST);
        }

        if (request.readingAt() == null) {
            throw new ApiException(
                    WATER_METER_READING_AT_REQUIRED,
                    HttpStatus.BAD_REQUEST);
        }

        if (request.ingestionKey() == null
                || request.ingestionKey().isBlank()) {
            throw new ApiException(
                    WATER_METER_INGESTION_KEY_REQUIRED,
                    HttpStatus.BAD_REQUEST);
        }
    }
}


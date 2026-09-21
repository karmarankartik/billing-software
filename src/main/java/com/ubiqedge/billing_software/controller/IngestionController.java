
package com.ubiqedge.billing_software.controller;

import com.ubiqedge.billing_software.constant.AppConstant.*;
import com.ubiqedge.billing_software.dto.ApiResponse;
import com.ubiqedge.billing_software.dto.WaterMeterReadingRequest;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.service.WaterMeterReadingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.ubiqedge.billing_software.constant.AppConstant.SUCCESS;

@RestController
@RequestMapping("/api/water-meter-readings")
public class IngestionController {

    private final WaterMeterReadingService waterMeterReadingService;

    public IngestionController(
            WaterMeterReadingService waterMeterReadingService) {
        this.waterMeterReadingService = waterMeterReadingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> ingest(
            @Valid @RequestBody WaterMeterReadingRequest request) {

        waterMeterReadingService.ingest(
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.CREATED,
                        null
                ));
    }
}

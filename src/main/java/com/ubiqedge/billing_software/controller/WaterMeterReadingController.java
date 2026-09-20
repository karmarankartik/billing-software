package com.ubiqedge.billing_software.controller;



import com.ubiqedge.billing_software.dto.CreateWaterMeterReadingRequest;
import com.ubiqedge.billing_software.dto.ApiResponse;
import com.ubiqedge.billing_software.service.WaterMeterReadingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.SUCCESS;

@RestController
@RequestMapping("/api/water-meters")
public class WaterMeterReadingController {

    private final WaterMeterReadingService waterMeterReadingService;

    public WaterMeterReadingController(
            WaterMeterReadingService waterMeterReadingService) {
        this.waterMeterReadingService = waterMeterReadingService;
    }

    @PostMapping("/{waterMeterId}/readings")
    public ResponseEntity<ApiResponse<Void>> ingestReading(
            @PathVariable UUID waterMeterId,
            @RequestBody CreateWaterMeterReadingRequest request) {

        waterMeterReadingService.ingestReading(
                waterMeterId,
                request
        );

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        null
                ));
    }
}


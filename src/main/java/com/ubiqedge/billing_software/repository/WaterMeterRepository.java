package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.WaterMeter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterMeterRepository extends JpaRepository<WaterMeter, UUID> {

    Optional<WaterMeter> findByIdAndDeletedAtIsNull(UUID id);

    Optional<WaterMeter> findByMeterNumberAndDeletedAtIsNull(String meterNumber);

    boolean existsByMeterNumberAndDeletedAtIsNull(String meterNumber);

    List<WaterMeter> findAllByDeletedAtIsNull();
}
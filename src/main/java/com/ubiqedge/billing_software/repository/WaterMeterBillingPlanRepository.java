package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.WaterMeterBillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterMeterBillingPlanRepository
        extends JpaRepository<WaterMeterBillingPlan, UUID> {

    Optional<WaterMeterBillingPlan> findByWaterMeterIdAndEffectiveToIsNull(
            UUID waterMeterId
    );

    List<WaterMeterBillingPlan> findByWaterMeterIdOrderByEffectiveFromDesc(
            UUID waterMeterId
    );

    @Query("""
        SELECT p
        FROM WaterMeterBillingPlan p
        WHERE p.waterMeterId = :waterMeterId
          AND p.effectiveFrom <= :pointInTime
          AND (p.effectiveTo IS NULL OR p.effectiveTo > :pointInTime)
        ORDER BY p.effectiveFrom DESC
        """)
    Optional<WaterMeterBillingPlan> findPlanAt(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("pointInTime") OffsetDateTime pointInTime
    );

    @Query("""
        SELECT COUNT(p)
        FROM WaterMeterBillingPlan p
        WHERE p.waterMeterId = :waterMeterId
          AND p.effectiveFrom < :periodEnd
          AND (p.effectiveTo IS NULL OR p.effectiveTo > :periodStart)
        """)
    long countOverlappingPlans(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("periodStart") OffsetDateTime periodStart,
            @Param("periodEnd") OffsetDateTime periodEnd
    );
}
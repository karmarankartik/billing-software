package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.WaterMeterBillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterMeterBillingPlanRepository
        extends JpaRepository<WaterMeterBillingPlan, UUID> {

    @Query(value = """
            SELECT *
            FROM water_meter_billing_plans
            WHERE water_meter_id = :waterMeterId
              AND effective_to IS NULL
              AND active_plan_key = :waterMeterId
            """, nativeQuery = true)
    Optional<WaterMeterBillingPlan> findActiveByWaterMeterId(
            @Param("waterMeterId") UUID waterMeterId
    );

    @Modifying
    @Query(value = """
            UPDATE water_meter_billing_plans
            SET effective_to = :effectiveTo,
                active_plan_key = NULL
            WHERE water_meter_id = :waterMeterId
              AND effective_to IS NULL
            """, nativeQuery = true)
    int closeActivePlan(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("effectiveTo") Instant effectiveTo
    );


    @Query(value = """
        SELECT *
        FROM water_meter_billing_plans
        WHERE water_meter_id = :waterMeterId
          AND effective_from < :periodEnd
          AND (effective_to IS NULL OR effective_to > :periodStart)
        ORDER BY effective_from
        """, nativeQuery = true)
    List<WaterMeterBillingPlan> findPlansForPeriod(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd
    );


}
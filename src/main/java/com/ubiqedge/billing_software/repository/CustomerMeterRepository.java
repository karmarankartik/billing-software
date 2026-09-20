package com.ubiqedge.billing_software.repository;


import com.ubiqedge.billing_software.entity.WaterMeter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerMeterRepository extends JpaRepository<WaterMeter, UUID> {

    @Query(value = """
            SELECT
                wm.id AS id,
                wm.meter_number AS meterNumber,
                wm.created_at AS createdAt,
                wm.updated_at AS updatedAt,
                wmbp.billing_plan_id AS billingPlanId
            FROM water_meters wm
            INNER JOIN water_meter_assignments wma
                ON wma.water_meter_id = wm.id
            INNER JOIN water_meter_billing_plans wmbp
                ON wmbp.water_meter_id = wm.id
               AND wmbp.effective_to IS NULL
               AND wmbp.active_plan_key = wm.id
            WHERE wma.user_id = :userId
              AND wma.unassigned_at IS NULL
              AND wma.active_assignment_key = wm.id
              AND wm.deleted_at IS NULL
            ORDER BY wma.assigned_at DESC
            """, nativeQuery = true)
    List<CustomerMeterProjection> findActiveMetersByUserId(
            @Param("userId") UUID userId
    );

    @Query(value = """
            SELECT
                wm.id AS id,
                wm.meter_number AS meterNumber,
                wm.created_at AS createdAt,
                wm.updated_at AS updatedAt,
                wmbp.billing_plan_id AS billingPlanId
            FROM water_meters wm
            INNER JOIN water_meter_assignments wma
                ON wma.water_meter_id = wm.id
            INNER JOIN water_meter_billing_plans wmbp
                ON wmbp.water_meter_id = wm.id
               AND wmbp.effective_to IS NULL
               AND wmbp.active_plan_key = wm.id
            WHERE wm.id = :waterMeterId
              AND wma.user_id = :userId
              AND wma.unassigned_at IS NULL
              AND wma.active_assignment_key = wm.id
              AND wm.deleted_at IS NULL
            """, nativeQuery = true)
    Optional<CustomerMeterProjection> findActiveMeterByUserId(
            @Param("waterMeterId") UUID waterMeterId,
            @Param("userId") UUID userId
    );
}

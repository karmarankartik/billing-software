package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.BillingPlanSlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingPlanSlabRepository extends JpaRepository<BillingPlanSlab, UUID> {


    List<BillingPlanSlab> findAllByBillingPlanIdAndDeletedAtIsNull( UUID billingPlanId );

    List<BillingPlanSlab>
    findByBillingPlanIdOrderByLowerBoundAsc(
            UUID billingPlanId
    );


    @Query(value = """ 
 SELECT bps.* FROM billing_plan_slabs bps 
               WHERE bps.billing_plan_id = :billingPlanId
                  AND bps.deleted_at IS NULL ORDER BY bps.lower_bound ASC """, nativeQuery = true) List<BillingPlanSlab> findActiveSlabsByBillingPlanId(@Param("billingPlanId") UUID billingPlanId );
}
package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.BillingPlanSlab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingPlanSlabRepository extends JpaRepository<BillingPlanSlab, UUID> {

    List<BillingPlanSlab> findByBillingPlanIdAndDeletedAtIsNullOrderByLowerBoundAsc(
            UUID billingPlanId
    );

    Optional<BillingPlanSlab> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByBillingPlanIdAndDeletedAtIsNull(UUID billingPlanId);

    List<BillingPlanSlab> findByBillingPlanIdAndDeletedAtIsNullAndLowerBoundLessThan(
            UUID billingPlanId,
            BigDecimal upperBound
    );
}
package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.BillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingPlanRepository extends JpaRepository<BillingPlan, UUID> {
    boolean existsByCodeAndDeletedAtIsNull(String code); Optional<BillingPlan>
    findByIdAndDeletedAtIsNull(UUID id);
    List<BillingPlan> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    boolean existsByIdAndActiveTrueAndDeletedAtIsNull(UUID billingPlanId);
}
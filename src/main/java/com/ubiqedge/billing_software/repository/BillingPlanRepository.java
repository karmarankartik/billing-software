package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.BillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillingPlanRepository extends JpaRepository<BillingPlan, UUID> {
/*
    Optional<BillingPlan> findByIdAndDeletedAtIsNull(UUID id);

    Optional<BillingPlan> findByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndDeletedAtIsNull(String code);*/

    boolean existsByIdAndActiveTrueAndDeletedAtIsNull(UUID id);
}
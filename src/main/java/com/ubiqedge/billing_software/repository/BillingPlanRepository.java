package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.BillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingPlanRepository extends JpaRepository<BillingPlan, UUID> {

    @Query(value = """
            SELECT *
            FROM billing_plans
            WHERE id = :id
              AND active = TRUE
              AND deleted_at IS NULL
            """, nativeQuery = true)
    Optional<BillingPlan> findActiveById(@Param("id") UUID id);

    @Query(value = """
            SELECT *
            FROM billing_plans
            WHERE active = TRUE
              AND deleted_at IS NULL
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<BillingPlan> findAllActive();

    @Query(value = """
            SELECT COUNT(*)
            FROM billing_plans
            WHERE code = :code
              AND deleted_at IS NULL
            """, nativeQuery = true)
    long countByCode(@Param("code") String code);

    @Query(value = """
            SELECT COUNT(*)
            FROM billing_plans
            WHERE code = :code
              AND id <> :id
              AND deleted_at IS NULL
            """, nativeQuery = true)
    long countByCodeAndIdNot(
            @Param("code") String code,
            @Param("id") UUID id
    );

    default boolean existsByCode(String code) {
        return countByCode(code) > 0;
    }

    default boolean existsByCodeAndIdNot(String code, UUID id) {
        return countByCodeAndIdNot(code, id) > 0;
    }
}

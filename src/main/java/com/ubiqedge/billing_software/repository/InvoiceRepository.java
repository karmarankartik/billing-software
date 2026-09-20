package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByWaterMeterIdAndUserIdAndBillingMonthAndBillingYear(
            UUID waterMeterId,
            UUID userId,
            Integer billingMonth,
            Integer billingYear
    );

    boolean existsByWaterMeterIdAndUserIdAndBillingMonthAndBillingYear(
            UUID waterMeterId,
            UUID userId,
            Integer billingMonth,
            Integer billingYear
    );

    List<Invoice> findByUserIdOrderByBillingYearDescBillingMonthDesc(
            UUID userId
    );

    List<Invoice> findByWaterMeterIdOrderByBillingYearDescBillingMonthDesc(
            UUID waterMeterId
    );
}
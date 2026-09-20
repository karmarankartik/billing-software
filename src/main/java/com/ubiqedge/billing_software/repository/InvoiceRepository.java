package com.ubiqedge.billing_software.repository;


import com.ubiqedge.billing_software.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    boolean existsByWaterMeterIdAndUserIdAndBillingMonthAndBillingYear(
            UUID waterMeterId,
            UUID userId,
            Integer billingMonth,
            Integer billingYear
    );

    @Query("""
            SELECT i
            FROM Invoice i
            WHERE i.userId = :userId
              AND i.waterMeterId = :waterMeterId
              AND i.billingMonth = :billingMonth
              AND i.billingYear = :billingYear
            """)
    Optional<Invoice> findCustomerMeterInvoice(
            @Param("userId") UUID userId,
            @Param("waterMeterId") UUID waterMeterId,
            @Param("billingMonth") Integer billingMonth,
            @Param("billingYear") Integer billingYear
    );

    @Query("""
            SELECT i
            FROM Invoice i
            WHERE i.userId = :userId
              AND i.billingMonth = :billingMonth
              AND i.billingYear = :billingYear
            ORDER BY i.waterMeterId
            """)
    List<Invoice> findCustomerInvoices(
            @Param("userId") UUID userId,
            @Param("billingMonth") Integer billingMonth,
            @Param("billingYear") Integer billingYear
    );

    @Query("""
            SELECT COALESCE(SUM(i.totalAmount), 0)
            FROM Invoice i
            WHERE i.userId = :userId
              AND i.billingMonth = :billingMonth
              AND i.billingYear = :billingYear
            """)
    BigDecimal getCustomerTotalAmount(
            @Param("userId") UUID userId,
            @Param("billingMonth") Integer billingMonth,
            @Param("billingYear") Integer billingYear
    );
}


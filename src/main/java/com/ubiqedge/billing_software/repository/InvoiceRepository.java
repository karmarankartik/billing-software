package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository
        extends JpaRepository<Invoice, UUID> {

    boolean existsByAssignmentIdAndBillingPeriodStartAndBillingPeriodEnd(
            UUID assignmentId,
            LocalDate billingPeriodStart,
            LocalDate billingPeriodEnd
    );

    Optional<Invoice> findByAssignmentIdAndBillingPeriodStartAndBillingPeriodEnd(
            UUID assignmentId,
            LocalDate billingPeriodStart,
            LocalDate billingPeriodEnd
    );

    @Query("""
        SELECT i, ii
        FROM Invoice i
        LEFT JOIN InvoiceItem ii
            ON ii.invoiceId = i.id
        WHERE i.userId = :userId
        ORDER BY i.billingPeriodStart DESC, ii.segmentStart ASC
        """)
    List<Object[]> findInvoicesWithItemsByUserId(
            @Param("userId") UUID userId
    );
}
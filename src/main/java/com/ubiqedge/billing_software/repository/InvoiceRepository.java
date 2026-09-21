package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
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
}
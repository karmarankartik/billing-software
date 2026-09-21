package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.BillingGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillingGenerationJobRepository
        extends JpaRepository<BillingGenerationJob, UUID> {

    Optional<BillingGenerationJob>
    findFirstByStatusOrderByStartedAtAsc(
            String status
    );
}
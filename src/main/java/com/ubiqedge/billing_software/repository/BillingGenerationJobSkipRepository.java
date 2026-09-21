package com.ubiqedge.billing_software.repository;

import com.ubiqedge.billing_software.entity.BillingGenerationJobSkip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BillingGenerationJobSkipRepository
        extends JpaRepository<BillingGenerationJobSkip, UUID> {

    List<BillingGenerationJobSkip>
    findByJobIdOrderByCreatedAtAsc(
            UUID jobId
    );
}
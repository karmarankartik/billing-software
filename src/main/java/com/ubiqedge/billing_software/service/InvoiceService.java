package com.ubiqedge.billing_software.service;

import com.ubiqedge.billing_software.dto.BillingGenerationJobResponse;
import com.ubiqedge.billing_software.dto.InvoiceGenerationResponse;
import com.ubiqedge.billing_software.dto.SkippedMeterResponse;
import com.ubiqedge.billing_software.dto.UserInvoiceGenerationResponse;
import com.ubiqedge.billing_software.entity.*;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final WaterMeterAssignmentRepository waterMeterAssignmentRepository;
    private final WaterMeterReadingRepository waterMeterReadingRepository;
    private final WaterMeterBillingPlanRepository waterMeterBillingPlanRepository;
    private final BillingPlanRepository billingPlanRepository;
    private final BillingPlanSlabRepository billingPlanSlabRepository;
    private final BillingGenerationJobRepository billingGenerationJobRepository;
    private final BillingGenerationJobSkipRepository billingGenerationJobSkipRepository;
    private final Executor invoiceGenerationExecutor;
    private final WaterMeterRepository waterMeterRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            WaterMeterAssignmentRepository waterMeterAssignmentRepository,
            WaterMeterReadingRepository waterMeterReadingRepository,
            WaterMeterBillingPlanRepository waterMeterBillingPlanRepository,
            BillingPlanRepository billingPlanRepository,
            BillingPlanSlabRepository billingPlanSlabRepository,
            BillingGenerationJobRepository billingGenerationJobRepository,
            BillingGenerationJobSkipRepository billingGenerationJobSkipRepository,
            Executor invoiceGenerationExecutor,
            WaterMeterRepository waterMeterRepository) {

        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.waterMeterAssignmentRepository = waterMeterAssignmentRepository;
        this.waterMeterReadingRepository = waterMeterReadingRepository;
        this.waterMeterBillingPlanRepository = waterMeterBillingPlanRepository;
        this.billingPlanRepository = billingPlanRepository;
        this.billingPlanSlabRepository = billingPlanSlabRepository;
        this.billingGenerationJobRepository = billingGenerationJobRepository;
        this.billingGenerationJobSkipRepository =
                billingGenerationJobSkipRepository;
        this.invoiceGenerationExecutor = invoiceGenerationExecutor;
        this.waterMeterRepository = waterMeterRepository;
    }

    // ============================================================
    // ADMIN - START BULK INVOICE GENERATION
    // ============================================================

    public InvoiceGenerationResponse startInvoiceGeneration(
            UserSession userSession,
            User loggedInUser,
            LocalDate from,
            LocalDate to) {

        validateAdmin(userSession, loggedInUser);
        validateBillingPeriod(from, to);

        Optional<BillingGenerationJob> runningJob =
                billingGenerationJobRepository
                        .findFirstByStatusOrderByStartedAtAsc("RUNNING");

        if (runningJob.isPresent()) {
            throw new ApiException(
                    BILLING_GENERATION_ALREADY_RUNNING,
                    HttpStatus.BAD_REQUEST
            );
        }

        Instant now = Instant.now();

        BillingGenerationJob job = new BillingGenerationJob();
        job.setBillingPeriodStart(from);
        job.setBillingPeriodEnd(to);
        job.setStatus("RUNNING");
        job.setMetersProcessed(0);
        job.setInvoicesGenerated(0);
        job.setMetersSkipped(0);
        job.setStartedAt(now);
        job.setCreatedAt(now);

        BillingGenerationJob savedJob =
                billingGenerationJobRepository.save(job);

        CompletableFuture.runAsync(
                () -> processInvoiceGeneration(
                        savedJob.getId(),
                        from,
                        to
                ),
                invoiceGenerationExecutor
        );

        return new InvoiceGenerationResponse(
                savedJob.getId(),
                savedJob.getStatus()
        );
    }

    // ============================================================
    // ADMIN - ASYNC BULK PROCESSING
    // ============================================================

    private void processInvoiceGeneration(
            UUID jobId,
            LocalDate from,
            LocalDate to) {

        try {
            Instant periodStart =
                    from.atStartOfDay(ZoneOffset.UTC).toInstant();

            Instant periodEnd =
                    to.plusDays(1)
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant();

            List<WaterMeterAssignment> assignments =
                    waterMeterAssignmentRepository
                            .findAssignmentsOverlappingPeriod(
                                    periodStart,
                                    periodEnd
                            );

            int metersProcessed = 0;
            int invoicesGenerated = 0;
            int metersSkipped = 0;

            for (WaterMeterAssignment assignment : assignments) {

                InvoiceGenerationAttempt attempt;

                try {
                    attempt = generateInvoiceForAssignment(
                            assignment,
                            from,
                            to
                    );
                } catch (Exception exception) {

                    attempt = InvoiceGenerationAttempt.skipped(
                            exception.getMessage() == null
                                    ? BILLING_GENERATION_FAILED
                                    : exception.getMessage()
                    );
                }

                metersProcessed++;

                if (attempt.successful()) {

                    invoicesGenerated++;

                } else {

                    metersSkipped++;

                    BillingGenerationJobSkip skip =
                            new BillingGenerationJobSkip();

                    skip.setJobId(jobId);
                    skip.setWaterMeterId(
                            assignment.getWaterMeterId()
                    );
                    skip.setReason(attempt.reason());
                    skip.setCreatedAt(Instant.now());

                    billingGenerationJobSkipRepository.save(skip);
                }

                updateJobProgress(
                        jobId,
                        metersProcessed,
                        invoicesGenerated,
                        metersSkipped
                );
            }

            completeJob(
                    jobId,
                    metersProcessed,
                    invoicesGenerated,
                    metersSkipped
            );

        } catch (Exception exception) {

            BillingGenerationJob job =
                    billingGenerationJobRepository.findById(jobId)
                            .orElse(null);

            if (job != null) {
                job.setStatus("FAILED");
                job.setCompletedAt(Instant.now());
                billingGenerationJobRepository.save(job);
            }
        }
    }

    private void updateJobProgress(
            UUID jobId,
            int metersProcessed,
            int invoicesGenerated,
            int metersSkipped) {

        BillingGenerationJob job =
                billingGenerationJobRepository.findById(jobId)
                        .orElseThrow(() -> new ApiException(
                                BILLING_GENERATION_JOB_NOT_FOUND,
                                HttpStatus.BAD_REQUEST
                        ));

        job.setMetersProcessed(metersProcessed);
        job.setInvoicesGenerated(invoicesGenerated);
        job.setMetersSkipped(metersSkipped);

        billingGenerationJobRepository.save(job);
    }

    private void completeJob(
            UUID jobId,
            int metersProcessed,
            int invoicesGenerated,
            int metersSkipped) {

        BillingGenerationJob job =
                billingGenerationJobRepository.findById(jobId)
                        .orElseThrow(() -> new ApiException(
                                BILLING_GENERATION_JOB_NOT_FOUND,
                                HttpStatus.BAD_REQUEST
                        ));

        job.setMetersProcessed(metersProcessed);
        job.setInvoicesGenerated(invoicesGenerated);
        job.setMetersSkipped(metersSkipped);
        job.setStatus("COMPLETED");
        job.setCompletedAt(Instant.now());

        billingGenerationJobRepository.save(job);
    }

    // ============================================================
    // ADMIN - GET GENERATION JOB
    // ============================================================

    public BillingGenerationJobResponse getGenerationJob(
            UserSession userSession,
            User loggedInUser,
            UUID jobId) {

        validateAdmin(userSession, loggedInUser);

        BillingGenerationJob job =
                billingGenerationJobRepository.findById(jobId)
                        .orElseThrow(() -> new ApiException(
                                BILLING_GENERATION_JOB_NOT_FOUND,
                                HttpStatus.BAD_REQUEST
                        ));

        List<SkippedMeterResponse> skippedMeters =
                billingGenerationJobSkipRepository
                        .findByJobIdOrderByCreatedAtAsc(jobId)
                        .stream()
                        .map(skip ->
                                new SkippedMeterResponse(
                                        skip.getWaterMeterId(),
                                        skip.getReason()
                                )
                        )
                        .toList();

        return new BillingGenerationJobResponse(
                job.getId(),
                job.getBillingPeriodStart(),
                job.getBillingPeriodEnd(),
                job.getStatus(),
                job.getMetersProcessed(),
                job.getInvoicesGenerated(),
                job.getMetersSkipped(),
                job.getStartedAt(),
                job.getCompletedAt(),
                skippedMeters
        );
    }

    // ============================================================
    // USER - VIEW / GET EXISTING INVOICES
    // ============================================================

    public List<Invoice> getInvoicesForUser(
            UserSession userSession,
            User loggedInUser,
            LocalDate from,
            LocalDate to) {

        validateUser(userSession, loggedInUser);
        validateBillingPeriod(from, to);

        Instant periodStart =
                from.atStartOfDay(ZoneOffset.UTC).toInstant();

        Instant periodEnd =
                to.plusDays(1)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();

        List<WaterMeterAssignment> assignments =
                waterMeterAssignmentRepository
                        .findAssignmentsForUserOverlappingPeriod(
                                loggedInUser.getId(),
                                periodStart,
                                periodEnd
                        );

        List<Invoice> invoices = new ArrayList<>();

        for (WaterMeterAssignment assignment : assignments) {

            invoiceRepository
                    .findByAssignmentIdAndBillingPeriodStartAndBillingPeriodEnd(
                            assignment.getId(),
                            from,
                            to
                    )
                    .ifPresent(invoices::add);
        }

        return invoices;
    }

    // ============================================================
    // USER - CUSTOM INVOICE GENERATION
    // ============================================================

    @Transactional
    public UserInvoiceGenerationResponse generateInvoicesForUser(
            UserSession userSession,
            User loggedInUser,
            LocalDate from,
            LocalDate to) {

        validateUser(userSession, loggedInUser);
        validateBillingPeriod(from, to);

        Instant periodStart =
                from.atStartOfDay(ZoneOffset.UTC).toInstant();

        Instant periodEnd =
                to.plusDays(1)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();

        List<WaterMeterAssignment> assignments =
                waterMeterAssignmentRepository
                        .findAssignmentsForUserOverlappingPeriod(
                                loggedInUser.getId(),
                                periodStart,
                                periodEnd
                        );

        List<Invoice> invoices = new ArrayList<>();
        List<SkippedMeterResponse> skippedMeters = new ArrayList<>();

        int metersProcessed = 0;
        int invoicesGenerated = 0;
        int metersSkipped = 0;

        for (WaterMeterAssignment assignment : assignments) {

            metersProcessed++;

            /*
             * Exact snapshot already exists.
             * Return the existing immutable invoice.
             */
            Invoice existingInvoice =
                    invoiceRepository
                            .findByAssignmentIdAndBillingPeriodStartAndBillingPeriodEnd(
                                    assignment.getId(),
                                    from,
                                    to
                            )
                            .orElse(null);

            if (existingInvoice != null) {

                invoices.add(existingInvoice);
                invoicesGenerated++;

                continue;
            }

            InvoiceGenerationAttempt attempt;

            try {

                attempt = generateInvoiceForAssignment(
                        assignment,
                        from,
                        to
                );

            } catch (DataIntegrityViolationException exception) {

                /*
                 * Another request may have created the same exact
                 * snapshot concurrently.
                 */
                Invoice concurrentInvoice =
                        invoiceRepository
                                .findByAssignmentIdAndBillingPeriodStartAndBillingPeriodEnd(
                                        assignment.getId(),
                                        from,
                                        to
                                )
                                .orElse(null);

                if (concurrentInvoice != null) {

                    invoices.add(concurrentInvoice);
                    invoicesGenerated++;

                    continue;
                }

                metersSkipped++;

                skippedMeters.add(
                        new SkippedMeterResponse(
                                assignment.getWaterMeterId(),
                                BILLING_GENERATION_FAILED
                        )
                );

                continue;

            } catch (Exception exception) {

                metersSkipped++;

                skippedMeters.add(
                        new SkippedMeterResponse(
                                assignment.getWaterMeterId(),
                                exception.getMessage() == null
                                        ? BILLING_GENERATION_FAILED
                                        : exception.getMessage()
                        )
                );

                continue;
            }

            if (!attempt.successful()) {

                metersSkipped++;

                skippedMeters.add(
                        new SkippedMeterResponse(
                                assignment.getWaterMeterId(),
                                attempt.reason()
                        )
                );

                continue;
            }

            Invoice savedInvoice =
                    invoiceRepository
                            .findByAssignmentIdAndBillingPeriodStartAndBillingPeriodEnd(
                                    assignment.getId(),
                                    from,
                                    to
                            )
                            .orElse(null);

            if (savedInvoice == null) {

                metersSkipped++;

                skippedMeters.add(
                        new SkippedMeterResponse(
                                assignment.getWaterMeterId(),
                                BILLING_GENERATION_FAILED
                        )
                );

                continue;
            }

            invoices.add(savedInvoice);
            invoicesGenerated++;
        }
        long totalMeters =
                waterMeterRepository.countMetersForUser(
                        loggedInUser.getId()
                );

        return new UserInvoiceGenerationResponse(
                invoices,
                (int) totalMeters,
                metersProcessed,
                invoicesGenerated,
                metersSkipped,
                skippedMeters
        );
    }

    // ============================================================
    // COMMON INVOICE GENERATION
    // ============================================================

    private InvoiceGenerationAttempt generateInvoiceForAssignment(
            WaterMeterAssignment assignment,
            LocalDate from,
            LocalDate to) {

        if (invoiceRepository
                .existsByAssignmentIdAndBillingPeriodStartAndBillingPeriodEnd(
                        assignment.getId(),
                        from,
                        to
                )) {

            return InvoiceGenerationAttempt.skipped(
                    INVOICE_ALREADY_EXISTS
            );
        }

        LocalDate assignmentStart =
                assignment.getAssignedAt()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();

        LocalDate assignmentEnd =
                assignment.getUnassignedAt() == null
                        ? to
                        : assignment.getUnassignedAt()
                          .atZone(ZoneOffset.UTC)
                          .toLocalDate()
                          .minusDays(1);

         assignmentStart =
                assignment.getAssignedAt()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();

         assignmentEnd =
                assignment.getUnassignedAt() == null
                        ? null
                        : assignment.getUnassignedAt()
                          .atZone(ZoneOffset.UTC)
                          .toLocalDate()
                          .minusDays(1);

        if (from.isBefore(assignmentStart)
                || (assignmentEnd != null && to.isAfter(assignmentEnd))) {

            return InvoiceGenerationAttempt.skipped(
                    BILLING_DATE_RANGE_OUT_OF_BOUNDS
            );
        }

        LocalDate calculationFrom = from;
        LocalDate calculationTo = to;


        if (calculationFrom.isAfter(calculationTo)) {

            return InvoiceGenerationAttempt.skipped(
                    ASSIGNMENT_NOT_ACTIVE_FOR_BILLING_PERIOD
            );
        }

        WaterMeterReading fromReading =
                findLatestTotalReading(
                        assignment.getWaterMeterId(),
                        calculationFrom
                );

        if (fromReading == null) {

            return InvoiceGenerationAttempt.skipped(
                    FROM_TOTAL_READING_NOT_FOUND
            );
        }

        WaterMeterReading toReading =
                findLatestTotalReading(
                        assignment.getWaterMeterId(),
                        calculationTo
                );

        if (toReading == null) {

            return InvoiceGenerationAttempt.skipped(
                    TO_TOTAL_READING_NOT_FOUND
            );
        }

        if (toReading.getReadingValue()
                .compareTo(fromReading.getReadingValue()) < 0) {

            return InvoiceGenerationAttempt.skipped(
                    INVALID_TOTAL_READING
            );
        }

        List<PlanPeriod> planPeriods =
                createPlanPeriods(
                        assignment.getWaterMeterId(),
                        calculationFrom,
                        calculationTo
                );

        if (planPeriods.isEmpty()) {

            return InvoiceGenerationAttempt.skipped(
                    BILLING_PLAN_NOT_FOUND
            );
        }

        List<InvoiceItemCalculation> calculations =
                calculateInvoiceItems(
                        assignment.getWaterMeterId(),
                        planPeriods
                );

        if (calculations.isEmpty()) {

            return InvoiceGenerationAttempt.skipped(
                    BILLING_PLAN_BOUNDARY_READING_NOT_FOUND
            );
        }

        BigDecimal totalConsumption =
                calculations.stream()
                        .map(InvoiceItemCalculation::consumption)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal expectedConsumption =
                toReading.getReadingValue()
                        .subtract(
                                fromReading.getReadingValue()
                        );

        if (totalConsumption.compareTo(expectedConsumption) != 0) {

            return InvoiceGenerationAttempt.skipped(
                    CONSUMPTION_CALCULATION_FAILED
            );
        }

        BigDecimal totalAmount =
                calculations.stream()
                        .map(InvoiceItemCalculation::amount)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .setScale(
                                4,
                                RoundingMode.HALF_UP
                        );

        Instant now = Instant.now();

        Invoice invoice = new Invoice();

        invoice.setUserId(assignment.getUserId());
        invoice.setWaterMeterId(assignment.getWaterMeterId());
        invoice.setAssignmentId(assignment.getId());
        invoice.setBillingPeriodStart(from);
        invoice.setBillingPeriodEnd(to);
        invoice.setTotalConsumption(totalConsumption);
        invoice.setTotalAmount(totalAmount);
        invoice.setGeneratedAt(now);
        invoice.setCreatedAt(now);

        Invoice savedInvoice =
                invoiceRepository.save(invoice);

        List<InvoiceItem> items = new ArrayList<>();

        for (InvoiceItemCalculation calculation : calculations) {

            InvoiceItem item = new InvoiceItem();

            item.setInvoiceId(savedInvoice.getId());

            item.setBillingPlanId(
                    calculation.billingPlanId()
            );

            item.setSegmentStart(
                    calculation.segmentStart()
            );

            item.setSegmentEnd(
                    calculation.segmentEnd()
            );

            item.setOpeningReading(
                    calculation.openingReading()
            );

            item.setClosingReading(
                    calculation.closingReading()
            );

            item.setConsumption(
                    calculation.consumption()
            );

            item.setAmount(
                    calculation.amount()
            );

            item.setCreatedAt(now);

            items.add(item);
        }

        invoiceItemRepository.saveAll(items);

        return InvoiceGenerationAttempt.generated();
    }

    // ============================================================
    // PLAN PERIOD CREATION
    // ============================================================

    private List<PlanPeriod> createPlanPeriods(
            UUID waterMeterId,
            LocalDate from,
            LocalDate to) {

        List<WaterMeterBillingPlan> mappings =
                waterMeterBillingPlanRepository
                        .findByWaterMeterIdOrderByEffectiveFromAsc(
                                waterMeterId
                        );

        if (mappings.isEmpty()) {
            return List.of();
        }

        List<LocalDate> boundaries = new ArrayList<>();

        boundaries.add(from);

        mappings.stream()
                .map(mapping ->
                        mapping.getEffectiveFrom()
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                )
                .filter(date -> date.isAfter(from))
                .filter(date -> date.isBefore(to))
                .forEach(boundaries::add);

        boundaries.add(to);

        boundaries = boundaries.stream()
                .distinct()
                .sorted()
                .toList();

        List<PlanPeriod> periods = new ArrayList<>();

        for (int i = 0; i < boundaries.size() - 1; i++) {

            LocalDate segmentStart = boundaries.get(i);
            LocalDate segmentEnd = boundaries.get(i + 1);

            WaterMeterBillingPlan mapping =
                    findPlanForDate(
                            mappings,
                            segmentStart
                    );

            if (mapping == null) {
                return List.of();
            }

            periods.add(
                    new PlanPeriod(
                            mapping.getBillingPlanId(),
                            segmentStart,
                            segmentEnd
                    )
            );
        }

        return periods;
    }

    private WaterMeterBillingPlan findPlanForDate(
            List<WaterMeterBillingPlan> mappings,
            LocalDate date) {

        return mappings.stream()
                .filter(mapping -> {

                    LocalDate effectiveFrom =
                            mapping.getEffectiveFrom()
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate();

                    if (effectiveFrom.isAfter(date)) {
                        return false;
                    }

                    if (mapping.getEffectiveTo() == null) {
                        return true;
                    }

                    LocalDate effectiveTo =
                            mapping.getEffectiveTo()
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate();

                    return date.isBefore(effectiveTo);
                })
                .max(
                        Comparator.comparing(
                                WaterMeterBillingPlan::getEffectiveFrom
                        )
                )
                .orElse(null);
    }

    // ============================================================
    // INVOICE ITEM CALCULATION
    // ============================================================

    private List<InvoiceItemCalculation> calculateInvoiceItems(
            UUID waterMeterId,
            List<PlanPeriod> planPeriods) {

        List<InvoiceItemCalculation> calculations =
                new ArrayList<>();

        for (PlanPeriod period : planPeriods) {

            WaterMeterReading openingReading =
                    findLatestTotalReading(
                            waterMeterId,
                            period.segmentStart()
                    );

            WaterMeterReading closingReading =
                    findLatestTotalReading(
                            waterMeterId,
                            period.segmentEnd()
                    );

            if (openingReading == null
                    || closingReading == null) {

                return List.of();
            }

            if (closingReading.getReadingValue()
                    .compareTo(openingReading.getReadingValue()) < 0) {

                return List.of();
            }

            BillingPlan billingPlan =
                    billingPlanRepository.findById(
                                    period.billingPlanId()
                            )
                            .orElse(null);

            if (billingPlan == null
                    || !billingPlan.isActive()
                    || billingPlan.getDeletedAt() != null) {

                return List.of();
            }

            BigDecimal consumption =
                    closingReading.getReadingValue()
                            .subtract(
                                    openingReading.getReadingValue()
                            );

            BigDecimal amount;

            if ("FIXED".equals(billingPlan.getPlanType())) {

                amount = consumption.multiply(
                        billingPlan.getPricePerUnit()
                );

            } else if ("SLAB".equals(billingPlan.getPlanType())) {

                List<BillingPlanSlab> slabs =
                        billingPlanSlabRepository
                                .findByBillingPlanIdOrderByLowerBoundAsc(
                                        billingPlan.getId()
                                );

                amount = calculateSlabAmount(
                        consumption,
                        slabs
                );

            } else {

                return List.of();
            }

            amount = amount.setScale(
                    4,
                    RoundingMode.HALF_UP
            );

            Instant segmentStart =
                    period.segmentStart()
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant();

            Instant segmentEnd =
                    period.segmentEnd()
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant();

            calculations.add(
                    new InvoiceItemCalculation(
                            period.billingPlanId(),
                            segmentStart,
                            segmentEnd,
                            openingReading.getReadingValue(),
                            closingReading.getReadingValue(),
                            consumption,
                            amount
                    )
            );
        }

        return calculations;
    }

    // ============================================================
    // READING
    // ============================================================

    private WaterMeterReading findLatestTotalReading(
            UUID waterMeterId,
            LocalDate date) {

        Instant dayStart =
                date.atStartOfDay(ZoneOffset.UTC)
                        .toInstant();

        Instant nextDayStart =
                date.plusDays(1)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();

        return waterMeterReadingRepository
                .findLatestTotalReadingForDay(
                        waterMeterId,
                        dayStart,
                        nextDayStart
                )
                .orElse(null);
    }

    // ============================================================
    // SLAB CALCULATION
    // ============================================================

    private BigDecimal calculateSlabAmount(
            BigDecimal consumption,
            List<BillingPlanSlab> slabs) {

        if (slabs == null || slabs.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal amount = BigDecimal.ZERO;

        for (BillingPlanSlab slab : slabs) {

            BigDecimal lowerBound =
                    slab.getLowerBound();

            BigDecimal upperBound =
                    slab.getUpperBound();

            BigDecimal slabUnits;

            if (upperBound == null) {

                if (consumption.compareTo(lowerBound) <= 0) {
                    break;
                }

                slabUnits =
                        consumption.subtract(lowerBound);

            } else {

                if (consumption.compareTo(lowerBound) <= 0) {
                    continue;
                }

                BigDecimal upperLimit =
                        consumption.compareTo(upperBound) < 0
                                ? consumption
                                : upperBound;

                slabUnits =
                        upperLimit.subtract(lowerBound);
            }

            if (slabUnits.compareTo(BigDecimal.ZERO) > 0) {

                amount = amount.add(
                        slabUnits.multiply(
                                slab.getPricePerUnit()
                        )
                );
            }
        }

        return amount;
    }

    // ============================================================
    // USER - INVOICE ITEMS
    // ============================================================

    public List<InvoiceItem> getInvoiceItems(
            UserSession userSession,
            User loggedInUser,
            UUID invoiceId) {

        validateUser(userSession, loggedInUser);

        Invoice invoice =
                invoiceRepository.findById(invoiceId)
                        .orElseThrow(() -> new ApiException(
                                INVOICE_NOT_FOUND,
                                HttpStatus.BAD_REQUEST
                        ));

        if (!invoice.getUserId().equals(loggedInUser.getId())) {
            throw new ApiException(
                    INVOICE_NOT_FOUND,
                    HttpStatus.BAD_REQUEST
            );
        }

        return invoiceItemRepository
                .findByInvoiceIdOrderBySegmentStartAsc(
                        invoiceId
                );
    }

    // ============================================================
    // VALIDATION
    // ============================================================

    private void validateAdmin(
            UserSession userSession,
            User loggedInUser) {

        if (userSession == null || loggedInUser == null) {
            throw new ApiException(
                    INVALID_SESSION,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!ROLE_ADMIN.equals(loggedInUser.getRole())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED.toString(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateUser(
            UserSession userSession,
            User loggedInUser) {

        if (userSession == null || loggedInUser == null) {
            throw new ApiException(
                    INVALID_SESSION,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!ROLE_ADMIN.equals(loggedInUser.getRole())
                && !ROLE_USER.equals(loggedInUser.getRole())) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED.toString(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateBillingPeriod(
            LocalDate from,
            LocalDate to) {

        if (from == null
                || to == null
                || !to.isAfter(from)) {

            throw new ApiException(
                    INVALID_BILLING_PERIOD,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    // ============================================================
    // INTERNAL RECORDS
    // ============================================================

    private record PlanPeriod(
            UUID billingPlanId,
            LocalDate segmentStart,
            LocalDate segmentEnd) {
    }

    private record InvoiceItemCalculation(
            UUID billingPlanId,
            Instant segmentStart,
            Instant segmentEnd,
            BigDecimal openingReading,
            BigDecimal closingReading,
            BigDecimal consumption,
            BigDecimal amount) {
    }

    private record InvoiceGenerationAttempt(
            boolean successful,
            String reason) {

        static InvoiceGenerationAttempt generated() {
            return new InvoiceGenerationAttempt(
                    true,
                    null
            );
        }

        static InvoiceGenerationAttempt skipped(
                String reason) {

            return new InvoiceGenerationAttempt(
                    false,
                    reason
            );
        }
    }
}
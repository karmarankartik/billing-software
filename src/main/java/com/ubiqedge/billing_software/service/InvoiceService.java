/*
package com.ubiqedge.billing_software.service;

import com.ubiqedge.billing_software.dto.InvoiceResponse;
import com.ubiqedge.billing_software.entity.BillingPlan;
import com.ubiqedge.billing_software.entity.Invoice;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.entity.WaterMeter;
import com.ubiqedge.billing_software.entity.WaterMeterAssignment;
import com.ubiqedge.billing_software.entity.WaterMeterReading;
import com.ubiqedge.billing_software.entity.WaterMeterBillingPlan;
import com.ubiqedge.billing_software.constant.ReadingType;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.repository.BillingPlanRepository;
import com.ubiqedge.billing_software.repository.InvoiceRepository;
import com.ubiqedge.billing_software.repository.WaterMeterAssignmentRepository;
import com.ubiqedge.billing_software.repository.WaterMeterBillingPlanRepository;
import com.ubiqedge.billing_software.repository.WaterMeterReadingRepository;
import com.ubiqedge.billing_software.repository.WaterMeterRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final WaterMeterRepository waterMeterRepository;
    private final WaterMeterAssignmentRepository waterMeterAssignmentRepository;
    private final WaterMeterReadingRepository waterMeterReadingRepository;
    private final WaterMeterBillingPlanRepository waterMeterBillingPlanRepository;
    private final BillingPlanRepository billingPlanRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            WaterMeterRepository waterMeterRepository,
            WaterMeterAssignmentRepository waterMeterAssignmentRepository,
            WaterMeterReadingRepository waterMeterReadingRepository,
            WaterMeterBillingPlanRepository waterMeterBillingPlanRepository,
            BillingPlanRepository billingPlanRepository) {

        this.invoiceRepository = invoiceRepository;
        this.waterMeterRepository = waterMeterRepository;
        this.waterMeterAssignmentRepository = waterMeterAssignmentRepository;
        this.waterMeterReadingRepository = waterMeterReadingRepository;
        this.waterMeterBillingPlanRepository = waterMeterBillingPlanRepository;
        this.billingPlanRepository = billingPlanRepository;
    }

    @Transactional
    public void generateInvoices(
            UserSession userSession,
            User user,
            int month,
            int year) {

        validateAdmin(userSession, user);
        validateBillingPeriod(month, year);

        List<WaterMeter> meters =
                waterMeterRepository.findAllActive();

        for (WaterMeter meter : meters) {
            generateInvoiceForMeter(
                    meter,
                    month,
                    year,
                    user.getId()
            );
        }
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getCustomerMeterInvoice(
            UserSession userSession,
            User user,
            UUID waterMeterId,
            int month,
            int year) {

        validateBillingPeriod(month, year);

        Invoice invoice =
                invoiceRepository.findCustomerMeterInvoice(
                        user.getId(),
                        waterMeterId,
                        month,
                        year
                ).orElseThrow(() -> new ApiException(
                        INVOICE_NOT_FOUND,
                        HttpStatus.BAD_REQUEST
                ));

        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getCustomerInvoices(
            UserSession userSession,
            User user,
            int month,
            int year) {

        validateBillingPeriod(month, year);

        return invoiceRepository.findCustomerInvoices(
                        user.getId(),
                        month,
                        year
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getCustomerTotalAmount(
            UserSession userSession,
            User user,
            int month,
            int year) {

        validateBillingPeriod(month, year);

        return invoiceRepository.getCustomerTotalAmount(
                user.getId(),
                month,
                year
        );
    }

    private void generateInvoiceForMeter(
            WaterMeter waterMeter,
            int month,
            int year,
            UUID generatedBy) {

        WaterMeterAssignment assignment =
                waterMeterAssignmentRepository
                        .findActiveByWaterMeterId(waterMeter.getId())
                        .orElse(null);

        if (assignment == null) {
            return;
        }

        UUID userId = assignment.getUserId();

        if (invoiceRepository
                .existsByWaterMeterIdAndUserIdAndBillingMonthAndBillingYear(
                        waterMeter.getId(),
                        userId,
                        month,
                        year)) {
            return;
        }

        YearMonth billingPeriod = YearMonth.of(year, month);

        Instant periodStart =
                billingPeriod.atDay(1)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();

        Instant periodEnd =
                billingPeriod.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();

        WaterMeterReading openingReading =
                waterMeterReadingRepository.findLatestReading(
                        waterMeter.getId(),
                        ReadingType.TOTAL,
                        periodStart
                ).orElseThrow(() -> new ApiException(
                        INSUFFICIENT_READING_DATA,
                        HttpStatus.BAD_REQUEST
                ));

        WaterMeterReading closingReading =
                waterMeterReadingRepository.findLatestReading(
                        waterMeter.getId(),
                        ReadingType.TOTAL,
                        periodEnd.minusNanos(1)
                ).orElseThrow(() -> new ApiException(
                        INSUFFICIENT_READING_DATA,
                        HttpStatus.BAD_REQUEST
                ));

        BigDecimal consumption =
                closingReading.getReadingValue()
                        .subtract(openingReading.getReadingValue());

        if (consumption.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(
                    INVALID_TOTAL_READING,
                    HttpStatus.BAD_REQUEST
            );
        }

        WaterMeterBillingPlan meterBillingPlan =
                waterMeterBillingPlanRepository
                        .findActiveByWaterMeterId(waterMeter.getId())
                        .orElseThrow(() -> new ApiException(
                                WATER_METER_BILLING_PLAN_NOT_FOUND,
                                HttpStatus.BAD_REQUEST
                        ));

        BillingPlan billingPlan =
                billingPlanRepository
                        .findActiveById(meterBillingPlan.getBillingPlanId())
                        .orElseThrow(() -> new ApiException(
                                BILLING_PLAN_NOT_FOUND,
                                HttpStatus.BAD_REQUEST
                        ));

        BigDecimal totalAmount =
                consumption
                        .multiply(billingPlan.getPricePerUnit())
                        .setScale(4, RoundingMode.HALF_UP);

        Invoice invoice = new Invoice();

        invoice.setWaterMeterId(waterMeter.getId());
        invoice.setUserId(userId);
        invoice.setBillingMonth(month);
        invoice.setBillingYear(year);
        invoice.setOpeningReading(
                openingReading.getReadingValue()
        );
        invoice.setClosingReading(
                closingReading.getReadingValue()
        );
        invoice.setConsumption(consumption);
        invoice.setBillingPlanId(billingPlan.getId());
        invoice.setPricePerUnit(billingPlan.getPricePerUnit());
        invoice.setTotalAmount(totalAmount);
        invoice.setGeneratedAt(Instant.now());
        invoice.setGeneratedBy(generatedBy);

        try {
            invoiceRepository.saveAndFlush(invoice);
        } catch (DataIntegrityViolationException exception) {
            */
/*
             * Another request may have generated the same invoice
             * concurrently. The database unique constraint protects
             * against duplicate invoices.
             *//*

        }
    }

    private void validateBillingPeriod(
            int month,
            int year) {

        if (month < 1 || month > 12 || year < 2000) {
            throw new ApiException(
                    INVALID_BILLING_PERIOD,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateAdmin(
            UserSession userSession,
            User user) {

        if (userSession == null || user == null) {
            throw new ApiException(
                    INVALID_SESSION,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!ROLE_ADMIN.equals(user.getRole())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED.toString(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private InvoiceResponse toResponse(Invoice invoice) {

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getWaterMeterId(),
                invoice.getUserId(),
                invoice.getBillingMonth(),
                invoice.getBillingYear(),
                invoice.getOpeningReading(),
                invoice.getClosingReading(),
                invoice.getConsumption(),
                invoice.getBillingPlanId(),
                invoice.getPricePerUnit(),
                invoice.getTotalAmount(),
                invoice.getGeneratedAt()
        );
    }
}*/

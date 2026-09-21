package com.ubiqedge.billing_software.validation;

import com.ubiqedge.billing_software.dto.GenerateInvoiceRequest;
import com.ubiqedge.billing_software.dto.MonthlyInvoiceGenerationRequest;
import com.ubiqedge.billing_software.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;

import static com.ubiqedge.billing_software.constant.AppConstant.INVALID_BILLING_MONTH;
import static com.ubiqedge.billing_software.constant.AppConstant.INVALID_BILLING_YEAR;

@Component
public class InvoiceGenerationRequestValidator {

    private static final int MAX_YEAR_DIFFERENCE = 2;

    public GenerateInvoiceRequest validateAndBuild(
            MonthlyInvoiceGenerationRequest request) {

        if (request == null || request.year() == null) {
            throw new ApiException(
                    INVALID_BILLING_YEAR,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (request.month() == null
                || request.month() < 1
                || request.month() > 12) {

            throw new ApiException(
                    INVALID_BILLING_MONTH,
                    HttpStatus.BAD_REQUEST
            );
        }

        LocalDate currentMonth =
                LocalDate.now().withDayOfMonth(1);

        LocalDate requestedMonth;

        try {
            requestedMonth = LocalDate.of(
                    request.year(),
                    request.month(),
                    1
            );
        } catch (DateTimeException exception) {
            throw new ApiException(
                    INVALID_BILLING_YEAR,
                    HttpStatus.BAD_REQUEST
            );
        }

        /*
         * Billing cycle must be completed.
         *
         * Example:
         * Today: 2026-10-10
         *
         * 2026-09 -> valid
         * 2026-10 -> invalid
         */
        if (!requestedMonth.isBefore(currentMonth)) {
            throw new ApiException(
                    INVALID_BILLING_MONTH,
                    HttpStatus.BAD_REQUEST
            );
        }

        /*
         * Billing month cannot be older than 2 years.
         *
         * Example:
         * Today: 2026-10-10
         *
         * Earliest allowed month: 2024-10
         *
         * 2024-10 -> valid
         * 2024-09 -> invalid
         */
        LocalDate earliestAllowedMonth =
                currentMonth.minusYears(MAX_YEAR_DIFFERENCE);

        if (requestedMonth.isBefore(earliestAllowedMonth)) {
            throw new ApiException(
                    INVALID_BILLING_YEAR,
                    HttpStatus.BAD_REQUEST
            );
        }

        LocalDate from = requestedMonth;
        LocalDate to = requestedMonth.plusMonths(1);

        return new GenerateInvoiceRequest(
                from,
                to
        );
    }
}
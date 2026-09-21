package com.ubiqedge.billing_software.validation;

import com.ubiqedge.billing_software.dto.GenerateInvoiceRequest;
import com.ubiqedge.billing_software.dto.MonthlyInvoiceGenerationRequest;
import com.ubiqedge.billing_software.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

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

        int currentYear =
                LocalDate.now().getYear();

        if (Math.abs(request.year() - currentYear)
                > MAX_YEAR_DIFFERENCE) {

            throw new ApiException(
                    INVALID_BILLING_YEAR,
                    HttpStatus.BAD_REQUEST
            );
        }

        LocalDate from =
                LocalDate.of(
                        request.year(),
                        request.month(),
                        1
                );

        /*
         * Billing period end is exclusive.
         *
         * January:
         * 2026-01-01 → 2026-02-01
         *
         * February 2024:
         * 2024-02-01 → 2024-03-01
         *
         * Leap year is handled automatically by LocalDate.
         */
        LocalDate to =
                from.plusMonths(1);

        return new GenerateInvoiceRequest(
                from,
                to
        );
    }
}
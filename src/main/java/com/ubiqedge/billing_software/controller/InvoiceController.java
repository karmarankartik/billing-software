package com.ubiqedge.billing_software.controller;

import com.ubiqedge.billing_software.dto.*;
import com.ubiqedge.billing_software.entity.Invoice;
import com.ubiqedge.billing_software.entity.InvoiceItem;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.exception.ApiException;
import com.ubiqedge.billing_software.service.InvoiceService;
import com.ubiqedge.billing_software.validation.InvoiceGenerationRequestValidator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.INVALID_BILLING_PERIOD;
import static com.ubiqedge.billing_software.constant.AppConstant.INVOICES_GENERATED_SUCCESSFULLY;

@RestController
@RequestMapping("/api")
public class InvoiceController {

    @Autowired
    InvoiceGenerationRequestValidator validator;

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // ============================================================
    // ADMIN - BULK GENERATE
    // ============================================================

    @PostMapping("/admin/invoices/generate")
    public ResponseEntity<ApiResponse<InvoiceGenerationResponse>>
    generateInvoices(
            @RequestAttribute("userSession")
            UserSession userSession,

            @RequestAttribute("user")
            User loggedInUser,

            @Valid
            @RequestBody
            MonthlyInvoiceGenerationRequest request) {

        GenerateInvoiceRequest generateInvoiceRequest =
                validator
                        .validateAndBuild(request);



        InvoiceGenerationResponse response =
                invoiceService.startInvoiceGeneration(
                        userSession,
                        loggedInUser,
                        generateInvoiceRequest.billingPeriodStart(),
                        generateInvoiceRequest.billingPeriodEnd()
                );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                        new ApiResponse<>(
                                true,
                                INVOICES_GENERATED_SUCCESSFULLY,
                                HttpStatus.ACCEPTED,
                                response
                        )
                );
    }

    // ============================================================
    // ADMIN - CHECK GENERATION JOB
    // ============================================================

    @GetMapping("/admin/invoices/generation-jobs/{jobId}")
    public ResponseEntity<ApiResponse<BillingGenerationJobResponse>>
    getGenerationJob(
            @RequestAttribute("userSession")
            UserSession userSession,

            @RequestAttribute("user")
            User loggedInUser,

            @PathVariable
            UUID jobId) {

        BillingGenerationJobResponse response =
                invoiceService.getGenerationJob(
                        userSession,
                        loggedInUser,
                        jobId
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Billing generation job fetched successfully",
                        HttpStatus.OK,
                        response
                )
        );
    }

// ============================================================
// USER - VIEW / GET INVOICES
// ============================================================

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<Invoice>>>
    getMyInvoices(
            @RequestAttribute("userSession")
            UserSession userSession,

            @RequestAttribute("user")
            User loggedInUser,

            @RequestParam
            LocalDate from,

            @RequestParam
            LocalDate to) {



        List<Invoice> invoices =
                invoiceService.getInvoicesForUser(
                        userSession,
                        loggedInUser,
                        from,
                        to
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Invoices fetched successfully",
                        HttpStatus.OK,
                        invoices
                )
        );
    }


// ============================================================
// USER - CUSTOM GENERATION
// ============================================================

    @PostMapping("/invoices/generate")
    public ResponseEntity<ApiResponse<UserInvoiceGenerationResponse>>
    generateMyInvoices(
            @RequestAttribute("userSession")
            UserSession userSession,

            @RequestAttribute("user")
            User loggedInUser,

            @Valid
            @RequestBody
            GenerateInvoiceRequest request) {

        if (!request.billingPeriodStart().isBefore(request.billingPeriodEnd()) || ChronoUnit.DAYS.between(request.billingPeriodStart(), request.billingPeriodEnd()) > 45)
            throw new ApiException(INVALID_BILLING_PERIOD, HttpStatus.BAD_REQUEST);

        UserInvoiceGenerationResponse response =
                invoiceService.generateInvoicesForUser(
                        userSession,
                        loggedInUser,
                        request.billingPeriodStart(),
                        request.billingPeriodEnd()
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Invoices generated successfully",
                        HttpStatus.OK,
                        response
                )
        );
    }

    // ============================================================
    // USER - INVOICE ITEMS
    // ============================================================

    @GetMapping("/invoices/{invoiceId}/items")
    public ResponseEntity<ApiResponse<List<InvoiceItem>>>
    getInvoiceItems(
            @RequestAttribute("userSession")
            UserSession userSession,

            @RequestAttribute("user")
            User loggedInUser,

            @PathVariable
            UUID invoiceId) {

        List<InvoiceItem> items =
                invoiceService.getInvoiceItems(
                        userSession,
                        loggedInUser,
                        invoiceId
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Invoice details fetched successfully",
                        HttpStatus.OK,
                        items
                )
        );
    }
}
/*
package com.ubiqedge.billing_software.controller;

import com.ubiqedge.billing_software.dto.InvoiceResponse;
import com.ubiqedge.billing_software.entity.User;
import com.ubiqedge.billing_software.entity.UserSession;
import com.ubiqedge.billing_software.dto.ApiResponse;
import com.ubiqedge.billing_software.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.ubiqedge.billing_software.constant.AppConstant.*;

@RestController
@RequestMapping("/api")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/invoices/generate")
    public ResponseEntity<ApiResponse<Void>> generateInvoices(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @RequestParam int month,
            @RequestParam int year) {

        invoiceService.generateInvoices(
                userSession,
                user,
                month,
                year
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        null
                )
        );
    }

    @GetMapping("/customer/meters/{waterMeterId}/bill")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getCustomerMeterInvoice(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @PathVariable UUID waterMeterId,
            @RequestParam int month,
            @RequestParam int year) {

        InvoiceResponse response =
                invoiceService.getCustomerMeterInvoice(
                        userSession,
                        user,
                        waterMeterId,
                        month,
                        year
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        response
                )
        );
    }

    @GetMapping("/customer/bill")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getCustomerInvoices(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @RequestParam int month,
            @RequestParam int year) {

        List<InvoiceResponse> response =
                invoiceService.getCustomerInvoices(
                        userSession,
                        user,
                        month,
                        year
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        response
                )
        );
    }

    @GetMapping("/customer/bill/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getCustomerTotalAmount(
            @RequestAttribute("userSession") UserSession userSession,
            @RequestAttribute("user") User user,
            @RequestParam int month,
            @RequestParam int year) {

        BigDecimal response =
                invoiceService.getCustomerTotalAmount(
                        userSession,
                        user,
                        month,
                        year
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        SUCCESS,
                        HttpStatus.OK,
                        response
                )
        );
    }
}*/

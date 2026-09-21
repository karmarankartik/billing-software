package com.ubiqedge.billing_software.mapper;


import com.ubiqedge.billing_software.dto.InvoiceItemResponse;
import com.ubiqedge.billing_software.dto.InvoiceResponse;
import com.ubiqedge.billing_software.entity.Invoice;
import com.ubiqedge.billing_software.entity.InvoiceItem;
import com.ubiqedge.billing_software.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InvoiceMapper {




        private final InvoiceRepository invoiceRepository;

        InvoiceMapper(InvoiceRepository invoiceRepository){
            this.invoiceRepository= invoiceRepository;
        }

        public List<InvoiceResponse> getInvoicesByUserId(UUID userId) {

            List<Object[]> rows =
                    invoiceRepository.findInvoicesWithItemsByUserId(userId);

            Map<UUID, InvoiceResponseBuilder> invoiceMap =
                    new LinkedHashMap<>();

            for (Object[] row : rows) {

                Invoice invoice = (Invoice) row[0];
                InvoiceItem item = (InvoiceItem) row[1];

                InvoiceResponseBuilder builder =
                        invoiceMap.computeIfAbsent(
                                invoice.getId(),
                                id -> new InvoiceResponseBuilder(invoice)
                        );

                if (item != null) {
                    builder.addItem(item);
                }
            }

            return invoiceMap.values()
                    .stream()
                    .map(InvoiceResponseBuilder::build)
                    .toList();
        }

        private static class InvoiceResponseBuilder {

            private final Invoice invoice;
            private final List<InvoiceItemResponse> items = new ArrayList<>();

            private InvoiceResponseBuilder(Invoice invoice) {
                this.invoice = invoice;
            }

            private void addItem(InvoiceItem item) {
                items.add(new InvoiceItemResponse(
                        item.getId(),
                        item.getBillingPlanId(),
                        item.getSegmentStart(),
                        item.getSegmentEnd(),
                        item.getOpeningReading(),
                        item.getClosingReading(),
                        item.getConsumption(),
                        item.getAmount(),
                        item.getCreatedAt()
                ));
            }

            private InvoiceResponse build() {
                return new InvoiceResponse(
                        invoice.getId(),
                        invoice.getUserId(),
                        invoice.getWaterMeterId(),
                        invoice.getAssignmentId(),
                        invoice.getBillingPeriodStart(),
                        invoice.getBillingPeriodEnd(),
                        invoice.getTotalConsumption(),
                        invoice.getTotalAmount(),
                        invoice.getGeneratedAt(),
                        invoice.getCreatedAt(),
                        items
                );
            }
        }
    }

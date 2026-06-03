package com.nexuserp.finance.adapter.in.rest;

import com.nexuserp.finance.application.command.CreateInvoiceCommand;
import com.nexuserp.finance.domain.model.Invoice;
import com.nexuserp.finance.domain.model.InvoiceLine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvoiceMapper {

    public CreateInvoiceCommand toCommand(CreateInvoiceRequest req, String tenantId, String userId) {
        List<CreateInvoiceCommand.LineCommand> lines = req.lines() == null ? List.of() :
            req.lines().stream().map(l -> new CreateInvoiceCommand.LineCommand(
                l.description(), l.productCode(), l.quantity(), l.unitPrice(),
                l.discountPct(), l.taxRate(), l.accountId(), l.costCenterId()
            )).toList();

        return new CreateInvoiceCommand(
            tenantId, req.invoiceType(), req.partnerId(), req.partnerName(), req.partnerVat(),
            req.invoiceDate(), req.dueDate(), req.currency(), req.notes(), userId, lines
        );
    }

    public InvoiceDto toDto(Invoice invoice) {
        List<InvoiceDto.InvoiceLineDto> lineDtos = invoice.getLines().stream()
            .map(this::toLineDto)
            .toList();

        return new InvoiceDto(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getType().name(),
            invoice.getStatus().name(),
            invoice.getPartnerId(),
            invoice.getPartnerName(),
            invoice.getPartnerVat(),
            invoice.getInvoiceDate(),
            invoice.getDueDate(),
            invoice.getCurrency(),
            invoice.getSubtotal() != null ? invoice.getSubtotal().amount() : null,
            invoice.getTaxAmount() != null ? invoice.getTaxAmount().amount() : null,
            invoice.getTotal() != null ? invoice.getTotal().amount() : null,
            invoice.getAmountPaid() != null ? invoice.getAmountPaid().amount() : null,
            invoice.getAmountDue() != null ? invoice.getAmountDue().amount() : null,
            invoice.getNotes(),
            lineDtos
        );
    }

    private InvoiceDto.InvoiceLineDto toLineDto(InvoiceLine line) {
        return new InvoiceDto.InvoiceLineDto(
            line.getId(), line.getLineNumber(), line.getDescription(), line.getProductCode(),
            line.getQuantity(), line.getUnitPrice(), line.getDiscountPct(), line.getTaxRate(),
            line.getSubtotal(), line.getTaxAmount(), line.getTotal()
        );
    }
}

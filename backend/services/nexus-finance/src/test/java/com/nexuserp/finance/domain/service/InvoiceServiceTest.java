package com.nexuserp.finance.domain.service;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.finance.application.command.RecordPaymentCommand;
import com.nexuserp.finance.domain.model.Invoice;
import com.nexuserp.finance.domain.model.InvoiceLine;
import com.nexuserp.finance.domain.port.out.InvoiceEventPublisher;
import com.nexuserp.finance.domain.port.out.InvoiceRepository;
import com.nexuserp.finance.domain.port.out.ProcessedPaymentStore;
import com.nexuserp.finance.infrastructure.compliance.ComplianceEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests du chemin "paiement externe" (Mobile Money via nexus-payment) :
 * application idempotente du règlement sur la facture.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceEventPublisher eventPublisher;
    @Mock private ComplianceEngine complianceEngine;
    @Mock private InvoiceNumberGenerator numberGenerator;
    @Mock private ProcessedPaymentStore processedPaymentStore;

    @InjectMocks private InvoiceService service;

    private static final String TENANT = "tenant-ci";

    private Invoice payableInvoiceXof() {
        return Invoice.builder()
            .tenantId(TENANT)
            .invoiceNumber("FA-2026-0001")
            .type(Invoice.InvoiceType.CUSTOMER)
            .currency("XOF")
            .createdBy("user-1")
            .addLine(InvoiceLine.builder()
                .lineNumber(1).description("Prestation").quantity(1).unitPrice(1000).taxRate(0)
                .build())
            .build();
    }

    @Test
    @DisplayName("Applique le règlement et marque le paiement comme traité quand non encore traité")
    void shouldApplyPayment_whenNotYetProcessed() {
        Invoice invoice = payableInvoiceXof();
        RecordPaymentCommand cmd = new RecordPaymentCommand(
            TENANT, invoice.getId(), "PAY-1", new BigDecimal("1000"), "XOF", "WAVE", "ext-1");

        when(processedPaymentStore.isProcessed("PAY-1", TENANT)).thenReturn(false);
        when(invoiceRepository.findById(eq(invoice.getId()), eq(TENANT))).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        service.recordExternalPayment(cmd);

        assertEquals(Invoice.InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(0, invoice.getAmountDue().amount().compareTo(BigDecimal.ZERO));
        verify(invoiceRepository).save(invoice);
        verify(processedPaymentStore).markProcessed("PAY-1", TENANT, invoice.getId().toString());
        verify(eventPublisher, times(1)).publishAll(any());
    }

    @Test
    @DisplayName("Ne ré-applique pas un paiement déjà traité (idempotence Kafka at-least-once)")
    void shouldBeIdempotent_whenAlreadyProcessed() {
        RecordPaymentCommand cmd = new RecordPaymentCommand(
            TENANT, UUID.randomUUID(), "PAY-DUP", new BigDecimal("1000"), "XOF", "ORANGE_MONEY", "ext-2");

        when(processedPaymentStore.isProcessed("PAY-DUP", TENANT)).thenReturn(true);

        service.recordExternalPayment(cmd);

        verifyNoInteractions(invoiceRepository);
        verify(processedPaymentStore, never()).markProcessed(anyString(), anyString(), anyString());
        verify(eventPublisher, never()).publishAll(any());
    }

    @Test
    @DisplayName("Lève une exception domaine quand la facture est introuvable")
    void shouldThrow_whenInvoiceNotFound() {
        UUID missing = UUID.randomUUID();
        RecordPaymentCommand cmd = new RecordPaymentCommand(
            TENANT, missing, "PAY-3", new BigDecimal("500"), "XOF", "MTN_MOMO", "ext-3");

        when(processedPaymentStore.isProcessed("PAY-3", TENANT)).thenReturn(false);
        when(invoiceRepository.findById(eq(missing), eq(TENANT))).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> service.recordExternalPayment(cmd));

        verify(processedPaymentStore, never()).markProcessed(anyString(), anyString(), anyString());
        verify(eventPublisher, never()).publishAll(any());
    }
}

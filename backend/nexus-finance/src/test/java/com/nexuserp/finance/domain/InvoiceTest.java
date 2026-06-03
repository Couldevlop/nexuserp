package com.nexuserp.finance.domain;

import com.nexuserp.core.domain.Money;
import com.nexuserp.finance.domain.model.Invoice;
import com.nexuserp.finance.domain.model.InvoiceLine;
import com.nexuserp.finance.domain.model.InvoiceStatus;
import com.nexuserp.finance.domain.event.InvoiceCreatedEvent;
import com.nexuserp.finance.domain.event.InvoiceValidatedEvent;
import com.nexuserp.finance.domain.event.InvoicePaidEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Invoice Aggregate")
class InvoiceTest {

    private Invoice.Builder baseBuilder;

    @BeforeEach
    void setUp() {
        baseBuilder = Invoice.builder()
                .id("inv-001")
                .invoiceNumber("FA-2026-ACME-000001")
                .tenantId("acme-corp")
                .customerId("cust-001")
                .customerName("Acme Corporation")
                .currency("EUR")
                .taxRate(new BigDecimal("20.00"))
                .invoiceDate(LocalDate.of(2026, 1, 15))
                .dueDate(LocalDate.of(2026, 2, 15))
                .lines(List.of(
                        InvoiceLine.builder()
                                .id("line-001")
                                .description("Consulting services")
                                .quantity(new BigDecimal("10"))
                                .unitPrice(new BigDecimal("150.00"))
                                .discountPercent(BigDecimal.ZERO)
                                .taxRate(new BigDecimal("20.00"))
                                .build()
                ));
    }

    @Test
    @DisplayName("Should create invoice in DRAFT status")
    void shouldCreateInvoice_inDraftStatus() {
        Invoice invoice = baseBuilder.build();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.getId()).isEqualTo("inv-001");
        assertThat(invoice.getCustomerName()).isEqualTo("Acme Corporation");
    }

    @Test
    @DisplayName("Should calculate subtotal correctly")
    void shouldCalculateSubtotal() {
        Invoice invoice = baseBuilder.build();
        // 10 * 150 * (1 - 0%) = 1500
        assertThat(invoice.getSubtotalAmount().amount()).isEqualByComparingTo("1500.0000");
    }

    @Test
    @DisplayName("Should calculate tax amount correctly")
    void shouldCalculateTaxAmount() {
        Invoice invoice = baseBuilder.build();
        // 1500 * 20% = 300
        assertThat(invoice.getTaxAmount().amount()).isEqualByComparingTo("300.0000");
    }

    @Test
    @DisplayName("Should calculate total amount correctly")
    void shouldCalculateTotalAmount() {
        Invoice invoice = baseBuilder.build();
        // 1500 + 300 = 1800
        assertThat(invoice.getTotalAmount().amount()).isEqualByComparingTo("1800.0000");
    }

    @Test
    @DisplayName("Should submit invoice from DRAFT to SUBMITTED")
    void shouldSubmit_fromDraftToSubmitted() {
        Invoice invoice = baseBuilder.build();
        invoice.submit("user-001");

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SUBMITTED);
        assertThat(invoice.getDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(InvoiceCreatedEvent.class);
    }

    @Test
    @DisplayName("Should approve invoice from SUBMITTED to APPROVED")
    void shouldApprove_fromSubmittedToApproved() {
        Invoice invoice = baseBuilder.build();
        invoice.submit("user-001");
        invoice.approve("manager-001");

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.APPROVED);
        assertThat(invoice.getDomainEvents())
                .anyMatch(e -> e instanceof InvoiceValidatedEvent);
    }

    @Test
    @DisplayName("Should throw when approving a DRAFT invoice")
    void shouldThrow_whenApprovingDraftInvoice() {
        Invoice invoice = baseBuilder.build();
        assertThatThrownBy(() -> invoice.approve("manager-001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUBMITTED");
    }

    @Test
    @DisplayName("Should record payment from APPROVED to PAID")
    void shouldRecordPayment_fromApprovedToPaid() {
        Invoice invoice = baseBuilder.build();
        invoice.submit("user-001");
        invoice.approve("manager-001");
        invoice.recordPayment(
                Money.of(new BigDecimal("1800.00"), "EUR"),
                LocalDate.of(2026, 2, 10),
                "user-001"
        );

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getDomainEvents())
                .anyMatch(e -> e instanceof InvoicePaidEvent);
    }

    @Test
    @DisplayName("Should throw when payment amount does not match total")
    void shouldThrow_whenPaymentAmountMismatch() {
        Invoice invoice = baseBuilder.build();
        invoice.submit("user-001");
        invoice.approve("manager-001");

        assertThatThrownBy(() -> invoice.recordPayment(
                Money.of(new BigDecimal("999.00"), "EUR"),
                LocalDate.now(),
                "user-001"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should cancel invoice from DRAFT")
    void shouldCancel_fromDraft() {
        Invoice invoice = baseBuilder.build();
        invoice.cancel("user-001", "Client request");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should throw when cancelling a PAID invoice")
    void shouldThrow_whenCancellingPaidInvoice() {
        Invoice invoice = baseBuilder.build();
        invoice.submit("user-001");
        invoice.approve("manager-001");
        invoice.recordPayment(
                Money.of(new BigDecimal("1800.00"), "EUR"),
                LocalDate.now(),
                "user-001"
        );
        assertThatThrownBy(() -> invoice.cancel("user-001", "reason"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should mark overdue when past due date and not paid")
    void shouldMarkOverdue_whenPastDueDate() {
        Invoice invoice = Invoice.builder()
                .id("inv-002")
                .invoiceNumber("FA-2026-ACME-000002")
                .tenantId("acme-corp")
                .customerId("cust-001")
                .customerName("Acme")
                .currency("EUR")
                .taxRate(new BigDecimal("20.00"))
                .invoiceDate(LocalDate.of(2025, 1, 1))
                .dueDate(LocalDate.of(2025, 2, 1))  // Past date
                .lines(List.of(
                        InvoiceLine.builder()
                                .id("line-x")
                                .description("Service")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(new BigDecimal("100"))
                                .discountPercent(BigDecimal.ZERO)
                                .taxRate(new BigDecimal("20.00"))
                                .build()
                ))
                .build();

        invoice.submit("user-001");
        invoice.markOverdue();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
    }

    @Test
    @DisplayName("Should apply discount correctly on line")
    void shouldApplyDiscount() {
        Invoice invoice = Invoice.builder()
                .id("inv-003")
                .invoiceNumber("FA-2026-ACME-000003")
                .tenantId("acme-corp")
                .customerId("cust-001")
                .customerName("Acme")
                .currency("EUR")
                .taxRate(new BigDecimal("20.00"))
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .lines(List.of(
                        InvoiceLine.builder()
                                .id("line-d")
                                .description("Discounted service")
                                .quantity(new BigDecimal("10"))
                                .unitPrice(new BigDecimal("100.00"))
                                .discountPercent(new BigDecimal("10.00")) // 10% discount
                                .taxRate(new BigDecimal("20.00"))
                                .build()
                ))
                .build();

        // subtotal = 10 * 100 * (1 - 0.10) = 900
        // tax = 900 * 0.20 = 180
        // total = 1080
        assertThat(invoice.getSubtotalAmount().amount()).isEqualByComparingTo("900.0000");
        assertThat(invoice.getTotalAmount().amount()).isEqualByComparingTo("1080.0000");
    }
}

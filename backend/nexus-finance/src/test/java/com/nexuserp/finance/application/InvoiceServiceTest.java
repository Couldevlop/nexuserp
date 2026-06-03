package com.nexuserp.finance.application;

import com.nexuserp.core.domain.TenantContext;
import com.nexuserp.finance.application.command.CreateInvoiceCommand;
import com.nexuserp.finance.application.usecase.ApproveInvoiceUseCase;
import com.nexuserp.finance.application.usecase.CreateInvoiceUseCase;
import com.nexuserp.finance.application.usecase.GetInvoiceUseCase;
import com.nexuserp.finance.domain.model.Invoice;
import com.nexuserp.finance.domain.model.InvoiceStatus;
import com.nexuserp.finance.domain.port.out.InvoiceEventPublisher;
import com.nexuserp.finance.domain.port.out.InvoiceRepository;
import com.nexuserp.finance.domain.service.ComplianceEngine;
import com.nexuserp.finance.domain.service.InvoiceNumberGenerator;
import com.nexuserp.finance.domain.service.InvoiceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService — Use Case Tests")
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceEventPublisher eventPublisher;
    @Mock private ComplianceEngine complianceEngine;
    @Mock private InvoiceNumberGenerator numberGenerator;

    @InjectMocks private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("fr-acme");
        TenantContext.setUserId("user-001");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ─── Create Invoice ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create invoice when all data is valid")
    void shouldCreateInvoice_whenValidData() {
        // Given
        when(numberGenerator.generate(anyString(), anyString(), anyInt()))
                .thenReturn("FA-2026-FR-ACME-000001");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(complianceEngine).validate(any(Invoice.class));

        CreateInvoiceCommand command = CreateInvoiceCommand.builder()
                .tenantId("fr-acme")
                .customerId("cust-001")
                .customerName("Acme France SARL")
                .currency("EUR")
                .taxRate(new BigDecimal("20.00"))
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .lines(List.of(
                        new CreateInvoiceCommand.LineCommand(
                                "Prestation conseil",
                                new BigDecimal("5"),
                                new BigDecimal("200.00"),
                                BigDecimal.ZERO,
                                new BigDecimal("20.00")
                        )
                ))
                .build();

        // When
        Invoice result = invoiceService.createInvoice(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomerName()).isEqualTo("Acme France SARL");
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        verify(invoiceRepository).save(any(Invoice.class));
        verify(complianceEngine).validate(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw when customer name is blank")
    void shouldThrow_whenCustomerNameIsBlank() {
        CreateInvoiceCommand command = CreateInvoiceCommand.builder()
                .tenantId("fr-acme")
                .customerId("cust-001")
                .customerName("")  // blank
                .currency("EUR")
                .taxRate(new BigDecimal("20.00"))
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .lines(List.of())
                .build();

        assertThatThrownBy(() -> invoiceService.createInvoice(command))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(invoiceRepository);
    }

    @Test
    @DisplayName("Should throw when lines list is empty")
    void shouldThrow_whenLinesEmpty() {
        CreateInvoiceCommand command = CreateInvoiceCommand.builder()
                .tenantId("fr-acme")
                .customerId("cust-001")
                .customerName("Client")
                .currency("EUR")
                .taxRate(new BigDecimal("20.00"))
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .lines(List.of())  // empty
                .build();

        assertThatThrownBy(() -> invoiceService.createInvoice(command))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should throw when compliance validation fails")
    void shouldThrow_whenComplianceValidationFails() {
        when(numberGenerator.generate(anyString(), anyString(), anyInt()))
                .thenReturn("FA-2026-FR-ACME-000001");
        doThrow(new RuntimeException("TVA rate 18.00 is not valid for country FR"))
                .when(complianceEngine).validate(any(Invoice.class));

        CreateInvoiceCommand command = CreateInvoiceCommand.builder()
                .tenantId("fr-acme")
                .customerId("cust-001")
                .customerName("Client")
                .currency("EUR")
                .taxRate(new BigDecimal("18.00"))  // Invalid for FR
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .lines(List.of(
                        new CreateInvoiceCommand.LineCommand("Service", BigDecimal.ONE,
                                new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("18.00"))
                ))
                .build();

        assertThatThrownBy(() -> invoiceService.createInvoice(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("TVA");

        verify(invoiceRepository, never()).save(any());
    }

    // ─── Approve Invoice ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should approve invoice when status is SUBMITTED")
    void shouldApproveInvoice_whenSubmitted() {
        Invoice invoice = buildSampleInvoice();
        invoice.submit("user-001");

        when(invoiceRepository.findById(eq("inv-001"), eq("fr-acme")))
                .thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Invoice result = invoiceService.approveInvoice("inv-001");
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.APPROVED);
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should throw when approving non-existent invoice")
    void shouldThrow_whenInvoiceNotFound() {
        when(invoiceRepository.findById(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.approveInvoice("non-existent"))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Invoice buildSampleInvoice() {
        return Invoice.builder()
                .id("inv-001")
                .invoiceNumber("FA-2026-FR-ACME-000001")
                .tenantId("fr-acme")
                .customerId("cust-001")
                .customerName("Test Client")
                .currency("EUR")
                .taxRate(new BigDecimal("20.00"))
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .lines(List.of(
                        com.nexuserp.finance.domain.model.InvoiceLine.builder()
                                .id("line-1")
                                .description("Service")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(new BigDecimal("500.00"))
                                .discountPercent(BigDecimal.ZERO)
                                .taxRate(new BigDecimal("20.00"))
                                .build()
                ))
                .build();
    }
}

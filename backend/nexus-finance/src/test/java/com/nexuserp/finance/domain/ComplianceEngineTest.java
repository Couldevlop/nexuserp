package com.nexuserp.finance.domain;

import com.nexuserp.finance.domain.service.ComplianceEngine;
import com.nexuserp.finance.domain.model.Invoice;
import com.nexuserp.finance.domain.model.InvoiceLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ComplianceEngine — Multi-jurisdictional tax validation")
class ComplianceEngineTest {

    private ComplianceEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ComplianceEngine();
    }

    // ─── France PCG ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("FR — Should accept valid TVA rate 20%")
    void fr_shouldAcceptTvaRate20() {
        Invoice invoice = buildInvoice("fr-acme", "EUR", new BigDecimal("20.00"));
        assertThatNoException().isThrownBy(() -> engine.validate(invoice));
    }

    @Test
    @DisplayName("FR — Should accept valid TVA rate 10%")
    void fr_shouldAcceptTvaRate10() {
        Invoice invoice = buildInvoice("fr-acme", "EUR", new BigDecimal("10.00"));
        assertThatNoException().isThrownBy(() -> engine.validate(invoice));
    }

    @Test
    @DisplayName("FR — Should accept valid TVA rate 5.5%")
    void fr_shouldAcceptTvaRate5_5() {
        Invoice invoice = buildInvoice("fr-acme", "EUR", new BigDecimal("5.5"));
        assertThatNoException().isThrownBy(() -> engine.validate(invoice));
    }

    @Test
    @DisplayName("FR — Should accept valid TVA rate 2.1%")
    void fr_shouldAcceptTvaRate2_1() {
        Invoice invoice = buildInvoice("fr-acme", "EUR", new BigDecimal("2.1"));
        assertThatNoException().isThrownBy(() -> engine.validate(invoice));
    }

    @Test
    @DisplayName("FR — Should accept TVA rate 0% (exempt)")
    void fr_shouldAcceptTvaRate0() {
        Invoice invoice = buildInvoice("fr-acme", "EUR", new BigDecimal("0.00"));
        assertThatNoException().isThrownBy(() -> engine.validate(invoice));
    }

    @Test
    @DisplayName("FR — Should reject invalid TVA rate 18%")
    void fr_shouldRejectTvaRate18() {
        Invoice invoice = buildInvoice("fr-acme", "EUR", new BigDecimal("18.00"));
        assertThatThrownBy(() -> engine.validate(invoice))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("TVA");
    }

    @Test
    @DisplayName("FR — Should reject XOF currency")
    void fr_shouldRejectXofCurrency() {
        Invoice invoice = buildInvoice("fr-acme", "XOF", new BigDecimal("20.00"));
        assertThatThrownBy(() -> engine.validate(invoice))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("currency");
    }

    // ─── SYSCOHADA / CI ───────────────────────────────────────────────────────

    @Test
    @DisplayName("CI — Should accept valid TVA rate 18% (UEMOA)")
    void ci_shouldAcceptTvaRate18() {
        Invoice invoice = buildInvoice("ci-acme", "XOF", new BigDecimal("18.00"));
        assertThatNoException().isThrownBy(() -> engine.validate(invoice));
    }

    @Test
    @DisplayName("CI — Should accept TVA rate 0% (exempt)")
    void ci_shouldAcceptTvaRate0() {
        Invoice invoice = buildInvoice("ci-acme", "XOF", new BigDecimal("0.00"));
        assertThatNoException().isThrownBy(() -> engine.validate(invoice));
    }

    @Test
    @DisplayName("CI — Should reject TVA rate 20% (invalid for UEMOA)")
    void ci_shouldRejectTvaRate20() {
        Invoice invoice = buildInvoice("ci-acme", "XOF", new BigDecimal("20.00"));
        assertThatThrownBy(() -> engine.validate(invoice))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("TVA");
    }

    @Test
    @DisplayName("CI — Should accept EUR currency (exports)")
    void ci_shouldAcceptEurCurrency() {
        Invoice invoice = buildInvoice("ci-acme", "EUR", new BigDecimal("0.00"));
        assertThatNoException().isThrownBy(() -> engine.validate(invoice));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Invoice buildInvoice(String tenantId, String currency, BigDecimal taxRate) {
        return Invoice.builder()
                .id("test-inv")
                .invoiceNumber("FA-2026-TEST-000001")
                .tenantId(tenantId)
                .customerId("cust-001")
                .customerName("Test Customer")
                .currency(currency)
                .taxRate(taxRate)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .lines(List.of(
                        InvoiceLine.builder()
                                .id("line-1")
                                .description("Test service")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(new BigDecimal("100.00"))
                                .discountPercent(BigDecimal.ZERO)
                                .taxRate(taxRate)
                                .build()
                ))
                .build();
    }
}

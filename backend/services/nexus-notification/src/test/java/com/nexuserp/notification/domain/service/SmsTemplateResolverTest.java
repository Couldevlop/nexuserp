package com.nexuserp.notification.domain.service;

import com.nexuserp.notification.domain.model.NotificationMessage.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SmsTemplateResolverTest {

    private final SmsTemplateResolver resolver = new SmsTemplateResolver();

    @Test
    @DisplayName("Should render French 2FA template with code variable")
    void shouldRenderFrench2faWithCode() {
        String text = resolver.render(NotificationType.TWO_FA_CODE, "fr-FR", Map.of("code", "123456"));

        assertThat(text).contains("votre code est 123456");
        assertThat(text).contains("Valable 5 min");
    }

    @Test
    @DisplayName("Should render English 2FA template with code variable")
    void shouldRenderEnglish2faWithCode() {
        String text = resolver.render(NotificationType.TWO_FA_CODE, "en-US", Map.of("code", "987654"));

        assertThat(text).contains("your code is 987654");
        assertThat(text).contains("Valid 5 min");
    }

    @Test
    @DisplayName("Should default to French when locale is null")
    void shouldDefaultToFrenchWhenLocaleNull() {
        String text = resolver.render(NotificationType.PAYMENT_RECEIVED, null, Map.of("amount", "1500 XOF"));

        assertThat(text).contains("paiement de 1500 XOF recu");
    }

    @Test
    @DisplayName("Should substitute multiple variables in invoice reminder")
    void shouldSubstituteMultipleVariables() {
        String text = resolver.render(NotificationType.INVOICE_DUE_REMINDER, "fr-FR",
            Map.of("invoiceNumber", "INV-001", "amount", "200 EUR", "dueDate", "2026-06-30"));

        assertThat(text).contains("INV-001").contains("200 EUR").contains("2026-06-30");
    }

    @Test
    @DisplayName("Should leave placeholder empty when variable is missing")
    void shouldLeavePlaceholderEmptyWhenMissing() {
        String text = resolver.render(NotificationType.TWO_FA_CODE, "fr-FR", Map.of());

        assertThat(text).doesNotContain("{code}");
        assertThat(text).contains("votre code est");
    }

    @Test
    @DisplayName("Should sanitize control characters / newlines from variables (A03)")
    void shouldSanitizeControlChars() {
        String text = resolver.render(NotificationType.PAYMENT_RECEIVED, "fr-FR",
            Map.of("amount", "100\nHACK\rINJECT"));

        assertThat(text).doesNotContain("\n").doesNotContain("\r");
        assertThat(text).contains("100 HACK INJECT");
    }

    @Test
    @DisplayName("Should render LOW_STOCK_ALERT and PAYSLIP_AVAILABLE templates")
    void shouldRenderStockAndPayslip() {
        String stock = resolver.render(NotificationType.LOW_STOCK_ALERT, "fr-FR",
            Map.of("productName", "Paracetamol", "quantity", "5"));
        String payslip = resolver.render(NotificationType.PAYSLIP_AVAILABLE, "en-US",
            Map.of("period", "May 2026"));

        assertThat(stock).contains("Paracetamol").contains("5");
        assertThat(payslip).contains("May 2026").contains("payslip");
    }
}

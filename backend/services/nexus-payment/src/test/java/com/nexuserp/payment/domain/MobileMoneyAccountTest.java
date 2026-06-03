package com.nexuserp.payment.domain;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.payment.domain.model.MobileMoneyAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MobileMoneyAccount — MSISDN validation")
class MobileMoneyAccountTest {

    @Test
    @DisplayName("Should accept valid Ivorian E.164 MSISDN")
    void shouldAccept_validE164() {
        MobileMoneyAccount acc = MobileMoneyAccount.of("+2250700000000");
        assertThat(acc.value()).isEqualTo("+2250700000000");
    }

    @Test
    @DisplayName("Should normalize national 10-digit number to +225")
    void shouldNormalize_nationalNumber() {
        MobileMoneyAccount acc = MobileMoneyAccount.of("0700000000");
        assertThat(acc.value()).isEqualTo("+2250700000000");
    }

    @Test
    @DisplayName("Should normalize 00 international prefix to +")
    void shouldNormalize_doubleZeroPrefix() {
        MobileMoneyAccount acc = MobileMoneyAccount.of("002250700000000");
        assertThat(acc.value()).isEqualTo("+2250700000000");
    }

    @Test
    @DisplayName("Should strip spaces, dashes and parentheses")
    void shouldStrip_formatting() {
        MobileMoneyAccount acc = MobileMoneyAccount.of("+225 07-00 00 00 00");
        assertThat(acc.value()).isEqualTo("+2250700000000");
    }

    @Test
    @DisplayName("Should mask the middle digits keeping prefix and last 2")
    void shouldMask_middleDigits() {
        MobileMoneyAccount acc = MobileMoneyAccount.of("+2250700000099");
        assertThat(acc.masked()).startsWith("+225");
        assertThat(acc.masked()).endsWith("99");
        assertThat(acc.masked()).contains("*");
        assertThat(acc.masked()).doesNotContain("070000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "abc", "+225", "+0700000000", "+22507000000000000000"})
    @DisplayName("Should reject invalid MSISDN values")
    void shouldReject_invalid(String invalid) {
        assertThatThrownBy(() -> MobileMoneyAccount.of(invalid))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("MSISDN");
    }

    @Test
    @DisplayName("Should reject null MSISDN")
    void shouldReject_null() {
        assertThatThrownBy(() -> MobileMoneyAccount.of(null))
            .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Should reject blank MSISDN")
    void shouldReject_blank() {
        assertThatThrownBy(() -> MobileMoneyAccount.of("   "))
            .isInstanceOf(DomainException.class);
    }
}

package com.nexuserp.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money Value Object")
class MoneyTest {

    @Test
    @DisplayName("Should create Money with valid amount and currency")
    void shouldCreateMoney_whenValidInput() {
        Money money = Money.of(new BigDecimal("1500.00"), "EUR");
        assertThat(money.amount()).isEqualByComparingTo("1500.0000");
        assertThat(money.currency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Should create zero money")
    void shouldCreateZeroMoney() {
        Money zero = Money.zero("EUR");
        assertThat(zero.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(zero.isZero()).isTrue();
    }

    @Test
    @DisplayName("Should throw when amount is null")
    void shouldThrow_whenAmountIsNull() {
        assertThatThrownBy(() -> Money.of(null, "EUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    @DisplayName("Should throw when currency is null")
    void shouldThrow_whenCurrencyIsNull() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("100"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }

    @Test
    @DisplayName("Should add two Money values with same currency")
    void shouldAdd_whenSameCurrency() {
        Money a = Money.of(new BigDecimal("1000.00"), "EUR");
        Money b = Money.of(new BigDecimal("500.50"), "EUR");
        Money result = a.add(b);
        assertThat(result.amount()).isEqualByComparingTo("1500.5000");
        assertThat(result.currency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Should throw when adding Money with different currencies")
    void shouldThrow_whenAddingDifferentCurrencies() {
        Money eur = Money.of(new BigDecimal("100"), "EUR");
        Money xof = Money.of(new BigDecimal("65000"), "XOF");
        assertThatThrownBy(() -> eur.add(xof))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }

    @Test
    @DisplayName("Should subtract two Money values")
    void shouldSubtract_whenSameCurrency() {
        Money a = Money.of(new BigDecimal("1000.00"), "EUR");
        Money b = Money.of(new BigDecimal("300.00"), "EUR");
        Money result = a.subtract(b);
        assertThat(result.amount()).isEqualByComparingTo("700.0000");
    }

    @Test
    @DisplayName("Should multiply Money by factor")
    void shouldMultiply() {
        Money price = Money.of(new BigDecimal("100.00"), "EUR");
        Money result = price.multiply(new BigDecimal("1.20"));
        assertThat(result.amount()).isEqualByComparingTo("120.0000");
    }

    @Test
    @DisplayName("Should negate Money")
    void shouldNegate() {
        Money money = Money.of(new BigDecimal("500"), "EUR");
        Money negated = money.negate();
        assertThat(negated.amount()).isEqualByComparingTo("-500.0000");
        assertThat(negated.isNegative()).isTrue();
    }

    @Test
    @DisplayName("Should compare Money values correctly")
    void shouldCompare() {
        Money a = Money.of(new BigDecimal("100"), "EUR");
        Money b = Money.of(new BigDecimal("200"), "EUR");
        assertThat(a.isLessThan(b)).isTrue();
        assertThat(b.isGreaterThan(a)).isTrue();
        assertThat(a.isLessThan(a)).isFalse();
    }

    @Test
    @DisplayName("Should check positive")
    void shouldCheckPositive() {
        assertThat(Money.of(new BigDecimal("1"), "EUR").isPositive()).isTrue();
        assertThat(Money.zero("EUR").isPositive()).isFalse();
        assertThat(Money.of(new BigDecimal("-1"), "EUR").isPositive()).isFalse();
    }

    @Test
    @DisplayName("Should scale amount to 4 decimal places")
    void shouldScaleTo4DecimalPlaces() {
        Money money = Money.of(new BigDecimal("100.12345678"), "EUR");
        assertThat(money.amount().scale()).isEqualTo(4);
    }
}

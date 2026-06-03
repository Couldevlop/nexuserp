package com.nexuserp.payment.adapter.in.rest;

import com.nexuserp.payment.domain.model.PaymentDirection;
import com.nexuserp.payment.domain.model.PaymentProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Requête d'initiation d'un paiement Mobile Money.
 *
 * A03 (Injection) : toutes les entrées sont validées par Jakarta Validation.
 * Le MSISDN est contraint par @Pattern (E.164 large) en plus de la validation
 * stricte du value object {@code MobileMoneyAccount}.
 */
public record InitiatePaymentRequest(
    @NotNull PaymentProvider provider,
    PaymentDirection direction,
    @NotNull
    @Pattern(regexp = "^\\+?[0-9 ().-]{8,20}$", message = "msisdn must be a valid phone number")
    String msisdn,
    @NotNull @Positive BigDecimal amount,
    @Size(min = 3, max = 3) String currency,
    UUID invoiceId,
    @Size(max = 255) String description
) {}

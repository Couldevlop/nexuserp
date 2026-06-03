package com.nexuserp.payment.domain.model;

/**
 * États du cycle de vie d'un paiement Mobile Money.
 *
 * Machine à états (A04 — Insecure Design : transitions explicites) :
 *   PENDING   -> INITIATED | FAILED | CANCELLED
 *   INITIATED -> SUCCEEDED | FAILED | CANCELLED
 *   SUCCEEDED -> REFUNDED
 *   FAILED / CANCELLED / REFUNDED = terminaux
 */
public enum PaymentStatus {
    PENDING,
    INITIATED,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    REFUNDED;

    public boolean isTerminal() {
        return this == FAILED || this == CANCELLED || this == REFUNDED;
    }
}

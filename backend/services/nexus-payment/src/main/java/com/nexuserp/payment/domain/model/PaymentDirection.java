package com.nexuserp.payment.domain.model;

/**
 * Sens du flux Mobile Money.
 * COLLECTION   : encaissement client (cash-in vers le marchand).
 * DISBURSEMENT : décaissement (cash-out vers un bénéficiaire).
 */
public enum PaymentDirection {
    COLLECTION,
    DISBURSEMENT
}

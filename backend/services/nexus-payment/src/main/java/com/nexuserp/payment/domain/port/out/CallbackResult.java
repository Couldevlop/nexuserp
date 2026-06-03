package com.nexuserp.payment.domain.port.out;

import java.math.BigDecimal;

/**
 * Résultat normalisé d'un webhook provider après parsing.
 * Le service domaine s'appuie uniquement sur cette vue, jamais sur le format brut du provider.
 *
 * @param outcome SUCCEEDED | FAILED | UNKNOWN (mappé depuis le statut provider)
 */
public record CallbackResult(
    String reference,
    String externalTxId,
    Outcome outcome,
    BigDecimal amount,
    String currency,
    String rawStatus,
    String failureReason
) {
    public enum Outcome { SUCCEEDED, FAILED, UNKNOWN }
}

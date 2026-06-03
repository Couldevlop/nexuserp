package com.nexuserp.payment.adapter.out.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.payment.domain.port.out.CallbackResult;
import com.nexuserp.payment.domain.port.out.PaymentInitiation;
import com.nexuserp.payment.domain.port.out.ProviderResponse;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Base des stratégies SIMULÉES (sandbox dev). Fournit :
 *  - vérification de signature HMAC-SHA256 commune (A02/A08),
 *  - parsing d'un payload de callback normalisé JSON,
 *  - génération d'une réponse d'initiation simulée.
 *
 * Les vrais providers n'ont PAS à hériter de cette classe : ils implémentent
 * directement {@link ProviderStrategy}. Cette base sert uniquement de simulateur
 * de référence (sandbox Orange/Wave/MTN/Moov) tant que les API réelles ne sont
 * pas branchées.
 *
 * Format JSON de callback simulé attendu :
 * { "reference": "...", "externalTxId": "...", "status": "SUCCESS|FAILED",
 *   "amount": 1000, "currency": "XOF", "reason": "..." }
 */
public abstract class AbstractSimulatedProviderStrategy implements ProviderStrategy {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper;
    protected final PaymentProviderProperties properties;

    protected AbstractSimulatedProviderStrategy(ObjectMapper objectMapper,
                                                PaymentProviderProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** Secret webhook lu depuis la config (jamais en dur — A02). */
    protected String webhookSecret() {
        PaymentProviderProperties.ProviderConfig cfg = properties.forProvider(provider().name());
        return cfg != null ? cfg.webhookSecret() : null;
    }

    @Override
    public ProviderResponse initiateCollection(PaymentInitiation initiation) {
        // Simulation : accepte toute demande valide, renvoie une providerRef + instructions.
        String providerRef = provider().name().toLowerCase() + "-" + UUID.randomUUID();
        String ussd = "Composez *144# et validez " + initiation.amount() + " " + initiation.currency();
        log.info("[SIMULATED {}] initiateCollection ref={} accepted providerRef={}",
            provider(), initiation.reference(), providerRef);
        return ProviderResponse.accepted(providerRef, null, ussd);
    }

    @Override
    public boolean verifyCallback(byte[] rawBody, String signatureHeader) {
        // A02/A08 : HMAC-SHA256 en temps constant contre le secret de config.
        return HmacSignatures.isValid(rawBody, signatureHeader, webhookSecret());
    }

    @Override
    public CallbackResult parseCallback(byte[] rawBody) {
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            String reference = text(node, "reference");
            String externalTxId = text(node, "externalTxId");
            String rawStatus = text(node, "status");
            String reason = text(node, "reason");
            BigDecimal amount = node.hasNonNull("amount") ? node.get("amount").decimalValue() : null;
            String currency = text(node, "currency");

            CallbackResult.Outcome outcome = mapOutcome(rawStatus);
            return new CallbackResult(reference, externalTxId, outcome, amount, currency, rawStatus, reason);
        } catch (Exception e) {
            log.warn("[SIMULATED {}] failed to parse callback body", provider(), e);
            return new CallbackResult(null, null, CallbackResult.Outcome.UNKNOWN, null, null, null, null);
        }
    }

    /** Mapping statut provider -> outcome normalisé. Surchargeable par provider. */
    protected CallbackResult.Outcome mapOutcome(String rawStatus) {
        if (rawStatus == null) return CallbackResult.Outcome.UNKNOWN;
        return switch (rawStatus.toUpperCase()) {
            case "SUCCESS", "SUCCEEDED", "COMPLETED", "PAID" -> CallbackResult.Outcome.SUCCEEDED;
            case "FAILED", "FAILURE", "DECLINED", "CANCELLED", "EXPIRED" -> CallbackResult.Outcome.FAILED;
            default -> CallbackResult.Outcome.UNKNOWN;
        };
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }
}

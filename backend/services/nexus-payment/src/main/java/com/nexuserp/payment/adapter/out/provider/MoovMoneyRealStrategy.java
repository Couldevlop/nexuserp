package com.nexuserp.payment.adapter.out.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.domain.port.out.CallbackResult;
import com.nexuserp.payment.domain.port.out.PaymentInitiation;
import com.nexuserp.payment.domain.port.out.ProviderResponse;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties;
import com.nexuserp.payment.infrastructure.config.ProviderConfigResolver;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stratégie RÉELLE Moov Money — adaptateur REST de collecte GÉNÉRIQUE piloté par config.
 *
 * Le schéma public de l'API Moov Africa variant selon le pays/intégrateur, cet adaptateur
 * reste volontairement générique et entièrement piloté par la configuration :
 *  - POST {baseUrl}/collections (Bearer apiKey)
 *    body { amount, currency, msisdn, reference, description }
 *    -> { transaction_id | id, status, payment_url? }.
 *
 * Lorsqu'un déploiement dispose du schéma exact, seul {@code baseUrl} (et les chemins via
 * env) change : aucun code à modifier.
 *
 * Webhook : HMAC-SHA256 du corps brut contre le webhook-secret, en temps constant (A08).
 *
 * Activation : MOOV_API_BASE_URL (real.base-url) + MOOV_API_KEY non vides.
 */
@Component
public class MoovMoneyRealStrategy extends AbstractRealProviderStrategy {

    public MoovMoneyRealStrategy(ObjectMapper objectMapper,
                                 ProviderConfigResolver configResolver,
                                 RestClient.Builder restClientBuilder) {
        super(objectMapper, configResolver, restClientBuilder);
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.MOOV_MONEY;
    }

    @Override
    public ProviderResponse initiateCollection(PaymentInitiation initiation) {
        PaymentProviderProperties.RealApiConfig r = real();
        try {
            String currency = (r.currency() != null && !r.currency().isBlank())
                ? r.currency() : initiation.currency();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("amount", initiation.amount().toPlainString());
            body.put("currency", currency);
            body.put("msisdn", initiation.msisdn());
            body.put("reference", initiation.reference());
            body.put("description", initiation.description());
            if (initiation.callbackUrl() != null) {
                body.put("callback_url", initiation.callbackUrl());
            }

            JsonNode resp = restClient.post()
                .uri(baseUrl() + "/collections")
                .header("Authorization", "Bearer " + r.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

            String txId = firstNonNull(text(resp, "transaction_id"), text(resp, "id"));
            String paymentUrl = firstNonNull(text(resp, "payment_url"), text(resp, "redirect_url"));
            log.info("[REAL MOOV_MONEY] collection created ref={} msisdn={} txId={}",
                initiation.reference(), maskMsisdn(initiation.msisdn()), txId);

            if (txId == null && paymentUrl == null) {
                return ProviderResponse.rejected("MOOV_NO_TRANSACTION");
            }
            return ProviderResponse.accepted(txId != null ? txId : initiation.reference(),
                paymentUrl,
                paymentUrl == null ? "Validez le paiement Moov Money sur votre téléphone" : null);
        } catch (Exception e) {
            log.error("[REAL MOOV_MONEY] initiateCollection failed ref={} : {}",
                initiation.reference(), e.getMessage());
            return ProviderResponse.rejected("MOOV_API_ERROR");
        }
    }

    /** A08 — HMAC-SHA256 du corps brut contre le webhook-secret, en temps constant. */
    @Override
    public boolean verifyCallback(byte[] rawBody, String signatureHeader) {
        return HmacSignatures.isValid(rawBody, signatureHeader, webhookSecret());
    }

    @Override
    public CallbackResult parseCallback(byte[] rawBody) {
        JsonNode node = readTree(rawBody);
        if (node == null) {
            return new CallbackResult(null, null, CallbackResult.Outcome.UNKNOWN, null, null, null, null);
        }
        String reference = firstNonNull(text(node, "reference"), text(node, "external_reference"));
        String externalTxId = firstNonNull(text(node, "transaction_id"), text(node, "id"));
        String rawStatus = text(node, "status");
        BigDecimal amount = decimal(node, "amount");
        String currency = text(node, "currency");
        return new CallbackResult(reference, externalTxId, mapOutcome(rawStatus),
            amount, currency, rawStatus, text(node, "message"));
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}

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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stratégie RÉELLE MTN MoMo — Collection API (requestToPay).
 *
 * Flux :
 *  1. Token : POST {baseUrl}/collection/token/ avec Basic base64(apiUser:apiUserKey)
 *     + Ocp-Apim-Subscription-Key -> access_token.
 *  2. requestToPay : POST {baseUrl}/collection/v1_0/requesttopay
 *     headers : Bearer token, X-Reference-Id (UUID), X-Target-Environment, Ocp-Apim-Subscription-Key
 *     body { amount, currency, externalId, payer{partyIdType:MSISDN, partyId}, payerMessage, payeeNote }
 *     -> 202 Accepted (le X-Reference-Id devient l'identifiant de transaction).
 *
 * Webhook : MTN peut être configuré avec un callback ; faute de schéma de signature
 * standard documenté, on retombe sur le webhook-secret HMAC-SHA256 (constant-time).
 *
 * Activation : MTN_API_USER + MTN_API_USER_KEY + MTN_SUBSCRIPTION_KEY non vides.
 */
@Component
public class MtnMomoRealStrategy extends AbstractRealProviderStrategy {

    public MtnMomoRealStrategy(ObjectMapper objectMapper,
                               ProviderConfigResolver configResolver,
                               RestClient.Builder restClientBuilder) {
        super(objectMapper, configResolver, restClientBuilder);
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.MTN_MOMO;
    }

    @Override
    public ProviderResponse initiateCollection(PaymentInitiation initiation) {
        PaymentProviderProperties.RealApiConfig r = real();
        try {
            String accessToken = fetchAccessToken(r);
            String referenceId = UUID.randomUUID().toString();
            String targetEnv = (r.targetEnvironment() != null && !r.targetEnvironment().isBlank())
                ? r.targetEnvironment() : "sandbox";
            String currency = (r.currency() != null && !r.currency().isBlank())
                ? r.currency() : initiation.currency();

            // MTN partyId = MSISDN sans le '+'.
            String partyId = initiation.msisdn() != null
                ? initiation.msisdn().replaceFirst("^\\+", "") : null;

            Map<String, Object> payer = new LinkedHashMap<>();
            payer.put("partyIdType", "MSISDN");
            payer.put("partyId", partyId);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("amount", initiation.amount().toPlainString());
            body.put("currency", currency);
            body.put("externalId", initiation.reference());
            body.put("payer", payer);
            body.put("payerMessage", truncate(initiation.description(), 160));
            body.put("payeeNote", initiation.reference());

            restClient.post()
                .uri(baseUrl() + "/collection/v1_0/requesttopay")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Reference-Id", referenceId)
                .header("X-Target-Environment", targetEnv)
                .header("Ocp-Apim-Subscription-Key", r.subscriptionKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

            log.info("[REAL MTN_MOMO] requestToPay accepted ref={} msisdn={} referenceId={}",
                initiation.reference(), maskMsisdn(initiation.msisdn()), referenceId);
            // X-Reference-Id = identifiant provider servant ensuite au status & au callback.
            return ProviderResponse.accepted(referenceId, null,
                "Validez le paiement Mobile Money sur votre téléphone MTN");
        } catch (Exception e) {
            log.error("[REAL MTN_MOMO] requestToPay failed ref={} : {}",
                initiation.reference(), e.getMessage());
            return ProviderResponse.rejected("MTN_API_ERROR");
        }
    }

    private String fetchAccessToken(PaymentProviderProperties.RealApiConfig r) {
        String basic = Base64.getEncoder().encodeToString(
            (r.apiUser() + ":" + r.apiUserKey()).getBytes(StandardCharsets.UTF_8));

        JsonNode token = restClient.post()
            .uri(baseUrl() + "/collection/token/")
            .header("Authorization", "Basic " + basic)
            .header("Ocp-Apim-Subscription-Key", r.subscriptionKey())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(JsonNode.class);

        String accessToken = text(token, "access_token");
        if (accessToken == null) {
            throw new IllegalStateException("MTN collection/token returned no access_token");
        }
        return accessToken;
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
        // MTN : { externalId, financialTransactionId, status, amount, currency, reason }
        String reference = text(node, "externalId");
        String externalTxId = firstNonNull(text(node, "financialTransactionId"), text(node, "referenceId"));
        String rawStatus = text(node, "status");
        BigDecimal amount = decimal(node, "amount");
        String currency = text(node, "currency");
        String reason = node.has("reason")
            ? (node.get("reason").isObject() ? text(node.get("reason"), "code") : text(node, "reason"))
            : null;
        return new CallbackResult(reference, externalTxId, mapOutcome(rawStatus),
            amount, currency, rawStatus, reason);
    }

    private static String truncate(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}

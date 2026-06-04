package com.nexuserp.payment.adapter.out.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.domain.port.out.CallbackResult;
import com.nexuserp.payment.domain.port.out.PaymentInitiation;
import com.nexuserp.payment.domain.port.out.ProviderResponse;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stratégie RÉELLE Wave — Checkout API.
 *
 * Flux :
 *  - Création session : POST {baseUrl}/v1/checkout/sessions (Bearer apiKey)
 *    body { amount, currency, error_url, success_url, client_reference }
 *    -> { id, wave_launch_url, ... }.
 *  - Le client est redirigé vers wave_launch_url.
 *
 * Webhook : Wave signe avec le header {@code Wave-Signature} = HMAC-SHA256 du corps brut
 * avec le webhook secret (format "t=...,v1=..."). Vérification en temps constant.
 *
 * Activation : WAVE_API_KEY non vide.
 */
@Component
public class WaveRealStrategy extends AbstractRealProviderStrategy {

    public WaveRealStrategy(ObjectMapper objectMapper,
                            PaymentProviderProperties properties,
                            RestClient.Builder restClientBuilder) {
        super(objectMapper, properties, restClientBuilder);
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.WAVE;
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
            body.put("client_reference", initiation.reference());
            if (r.returnUrl() != null && !r.returnUrl().isBlank()) {
                body.put("success_url", r.returnUrl());
            }
            if (r.cancelUrl() != null && !r.cancelUrl().isBlank()) {
                body.put("error_url", r.cancelUrl());
            }

            JsonNode resp = restClient.post()
                .uri(baseUrl() + "/v1/checkout/sessions")
                .header("Authorization", "Bearer " + r.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

            String sessionId = text(resp, "id");
            String launchUrl = text(resp, "wave_launch_url");
            log.info("[REAL WAVE] checkout session created ref={} msisdn={} sessionId={}",
                initiation.reference(), maskMsisdn(initiation.msisdn()), sessionId);

            if (launchUrl == null) {
                return ProviderResponse.rejected("WAVE_NO_LAUNCH_URL");
            }
            return ProviderResponse.accepted(sessionId, launchUrl, null);
        } catch (Exception e) {
            log.error("[REAL WAVE] initiateCollection failed ref={} : {}",
                initiation.reference(), e.getMessage());
            return ProviderResponse.rejected("WAVE_API_ERROR");
        }
    }

    /**
     * A08 — Vérifie le header {@code Wave-Signature}.
     * Format Wave : "t=<timestamp>,v1=<hex hmac>" où hmac = HMAC-SHA256(secret, "t.body").
     * Compatibilité : si le header est un simple hex, on compare au HMAC du corps brut.
     * Comparaison en TEMPS CONSTANT (MessageDigest.isEqual).
     */
    @Override
    public boolean verifyCallback(byte[] rawBody, String signatureHeader) {
        String secret = webhookSecret();
        if (signatureHeader == null || signatureHeader.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        String timestamp = null;
        String v1 = null;
        for (String part : signatureHeader.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0].trim()) {
                case "t" -> timestamp = kv[1].trim();
                case "v1" -> v1 = kv[1].trim();
                default -> { /* ignore */ }
            }
        }
        if (timestamp != null && v1 != null) {
            // signed payload = "<timestamp>." + body
            byte[] signed = (timestamp + ".").getBytes(StandardCharsets.UTF_8);
            byte[] payload = concat(signed, rawBody);
            String expected = HmacSignatures.hmacSha256Hex(payload, secret);
            return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                v1.getBytes(StandardCharsets.UTF_8));
        }
        // Fallback : header = hex hmac du corps brut.
        return HmacSignatures.isValid(rawBody, signatureHeader, secret);
    }

    @Override
    public CallbackResult parseCallback(byte[] rawBody) {
        JsonNode root = readTree(rawBody);
        if (root == null) {
            return new CallbackResult(null, null, CallbackResult.Outcome.UNKNOWN, null, null, null, null);
        }
        // Wave : { type, data: { id, client_reference, payment_status, amount, currency } }
        JsonNode data = root.has("data") ? root.get("data") : root;
        String reference = text(data, "client_reference");
        String externalTxId = text(data, "id");
        String rawStatus = firstNonNull(text(data, "payment_status"), text(root, "type"));
        BigDecimal amount = decimal(data, "amount");
        String currency = text(data, "currency");
        return new CallbackResult(reference, externalTxId, mapOutcome(rawStatus),
            amount, currency, rawStatus, null);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    @Override
    protected CallbackResult.Outcome mapOutcome(String rawStatus) {
        if (rawStatus == null) return CallbackResult.Outcome.UNKNOWN;
        // Wave-specific statuses.
        return switch (rawStatus.toLowerCase()) {
            case "succeeded", "success", "checkout.session.completed", "completed" ->
                CallbackResult.Outcome.SUCCEEDED;
            case "cancelled", "expired", "failed", "checkout.session.payment_failed" ->
                CallbackResult.Outcome.FAILED;
            default -> super.mapOutcome(rawStatus);
        };
    }
}

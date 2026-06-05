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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/**
 * Stratégie RÉELLE Orange Money — Web Payment (OM WebPay API).
 *
 * Flux :
 *  1. OAuth2 client-credentials : POST {tokenUrl} (Basic base64(clientId:clientSecret))
 *     -> access_token.
 *  2. Création paiement : POST {baseUrl}/orange-money-webpay/dev/v1/webpayment
 *     (Bearer token) avec merchant_key, amount, currency, order_id, return/cancel/notif urls
 *     -> { payment_url, pay_token, notif_token }.
 *
 * Webhook : Orange notifie sur notif_url ; l'authenticité du callback repose sur le
 * notif_token (secret partagé) renvoyé à l'initiation. Faute de notif_token transmis
 * par référence ici, on retombe sur le webhook-secret HMAC partagé (constant-time).
 *
 * Activation : ORANGE_CLIENT_ID + ORANGE_CLIENT_SECRET + ORANGE_MERCHANT_KEY non vides.
 */
@Component
public class OrangeMoneyRealStrategy extends AbstractRealProviderStrategy {

    public OrangeMoneyRealStrategy(ObjectMapper objectMapper,
                                   ProviderConfigResolver configResolver,
                                   RestClient.Builder restClientBuilder) {
        super(objectMapper, configResolver, restClientBuilder);
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.ORANGE_MONEY;
    }

    @Override
    public ProviderResponse initiateCollection(PaymentInitiation initiation) {
        PaymentProviderProperties.RealApiConfig r = real();
        try {
            String accessToken = fetchAccessToken(r);

            String currency = (r.currency() != null && !r.currency().isBlank())
                ? r.currency() : initiation.currency();
            String notifUrl = initiation.callbackUrl();

            Map<String, Object> body = Map.of(
                "merchant_key", r.merchantKey(),
                "currency", currency,
                "order_id", initiation.reference(),
                "amount", initiation.amount(),
                "return_url", nullSafe(r.returnUrl()),
                "cancel_url", nullSafe(r.cancelUrl()),
                "notif_url", nullSafe(notifUrl),
                "lang", "fr",
                "reference", initiation.description() != null ? initiation.description() : initiation.reference()
            );

            JsonNode resp = restClient.post()
                .uri(baseUrl() + "/orange-money-webpay/dev/v1/webpayment")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

            String payToken = text(resp, "pay_token");
            String paymentUrl = text(resp, "payment_url");
            log.info("[REAL ORANGE_MONEY] webpayment created ref={} msisdn={} accepted",
                initiation.reference(), maskMsisdn(initiation.msisdn()));

            if (paymentUrl == null && payToken == null) {
                return ProviderResponse.rejected("ORANGE_NO_PAYMENT_URL");
            }
            return ProviderResponse.accepted(payToken != null ? payToken : initiation.reference(),
                paymentUrl, null);
        } catch (Exception e) {
            log.error("[REAL ORANGE_MONEY] initiateCollection failed ref={} : {}",
                initiation.reference(), e.getMessage());
            return ProviderResponse.rejected("ORANGE_API_ERROR");
        }
    }

    private String fetchAccessToken(PaymentProviderProperties.RealApiConfig r) {
        String tokenUrl = (r.tokenUrl() != null && !r.tokenUrl().isBlank())
            ? r.tokenUrl() : baseUrl() + "/oauth/v3/token";
        String basic = Base64.getEncoder().encodeToString(
            (r.clientId() + ":" + r.clientSecret()).getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        JsonNode token = restClient.post()
            .uri(tokenUrl)
            .header("Authorization", "Basic " + basic)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .accept(MediaType.APPLICATION_JSON)
            .body(form)
            .retrieve()
            .body(JsonNode.class);

        String accessToken = text(token, "access_token");
        if (accessToken == null) {
            throw new IllegalStateException("Orange OAuth2 returned no access_token");
        }
        return accessToken;
    }

    /**
     * A08 — Authenticité du webhook Orange. On accepte :
     *  - un header HMAC-SHA256 (sha256=...) du corps brut contre le webhook-secret,
     *  - OU une égalité constant-time directe avec le notif_token (secret partagé).
     */
    @Override
    public boolean verifyCallback(byte[] rawBody, String signatureHeader) {
        String secret = webhookSecret();
        if (signatureHeader == null || signatureHeader.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        if (HmacSignatures.isValid(rawBody, signatureHeader, secret)) {
            return true;
        }
        // notif_token direct (constant-time).
        return MessageDigest.isEqual(
            signatureHeader.getBytes(StandardCharsets.UTF_8),
            secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public CallbackResult parseCallback(byte[] rawBody) {
        JsonNode node = readTree(rawBody);
        if (node == null) {
            return new CallbackResult(null, null, CallbackResult.Outcome.UNKNOWN, null, null, null, null);
        }
        String reference = firstNonNull(text(node, "order_id"), text(node, "reference"));
        String externalTxId = firstNonNull(text(node, "txnid"), text(node, "pay_token"));
        String rawStatus = text(node, "status");
        BigDecimal amount = decimal(node, "amount");
        String currency = text(node, "currency");
        return new CallbackResult(reference, externalTxId, mapOutcome(rawStatus),
            amount, currency, rawStatus, text(node, "message"));
    }

    private static String nullSafe(String v) {
        return v == null ? "" : v;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}

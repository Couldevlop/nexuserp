package com.nexuserp.payment.adapter.out.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.payment.domain.port.out.CallbackResult;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Base des stratégies RÉELLES (appels aux API providers via {@link RestClient}).
 *
 * Mutualise :
 *  - accès à la config réelle ({@link PaymentProviderProperties.RealApiConfig}),
 *  - le {@link RestClient} (timeouts bornés),
 *  - le mapping de statut normalisé,
 *  - les helpers JSON.
 *
 * OWASP :
 *  - A02/A09 : ne JAMAIS logger les secrets ni le MSISDN complet (utiliser maskMsisdn).
 *  - A08 : verifyCallback doit comparer en temps constant (cf. implémentations).
 */
public abstract class AbstractRealProviderStrategy implements ProviderStrategy {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper;
    protected final PaymentProviderProperties properties;
    protected final RestClient restClient;

    protected AbstractRealProviderStrategy(ObjectMapper objectMapper,
                                           PaymentProviderProperties properties,
                                           RestClient.Builder restClientBuilder) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    protected PaymentProviderProperties.ProviderConfig config() {
        return properties.forProvider(provider().name());
    }

    protected PaymentProviderProperties.RealApiConfig real() {
        PaymentProviderProperties.ProviderConfig cfg = config();
        return cfg != null ? cfg.real() : null;
    }

    /** Base URL réelle : real.baseUrl si présent, sinon apiBaseUrl du provider. */
    protected String baseUrl() {
        PaymentProviderProperties.ProviderConfig cfg = config();
        if (cfg == null) return null;
        PaymentProviderProperties.RealApiConfig r = cfg.real();
        if (r != null && r.baseUrl() != null && !r.baseUrl().isBlank()) {
            return stripTrailingSlash(r.baseUrl());
        }
        return stripTrailingSlash(cfg.apiBaseUrl());
    }

    protected String webhookSecret() {
        PaymentProviderProperties.ProviderConfig cfg = config();
        return cfg != null ? cfg.webhookSecret() : null;
    }

    protected static String stripTrailingSlash(String url) {
        if (url == null) return null;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** A09 — masque le MSISDN pour les logs : conserve indicatif + 2 derniers chiffres. */
    protected static String maskMsisdn(String msisdn) {
        if (msisdn == null) return "null";
        int keepStart = Math.min(4, msisdn.length());
        int keepEnd = 2;
        if (msisdn.length() <= keepStart + keepEnd) {
            return "*".repeat(msisdn.length());
        }
        return msisdn.substring(0, keepStart)
            + "*".repeat(msisdn.length() - keepStart - keepEnd)
            + msisdn.substring(msisdn.length() - keepEnd);
    }

    protected CallbackResult.Outcome mapOutcome(String rawStatus) {
        if (rawStatus == null) return CallbackResult.Outcome.UNKNOWN;
        return switch (rawStatus.toUpperCase()) {
            case "SUCCESS", "SUCCESSFUL", "SUCCEEDED", "COMPLETED", "PAID", "ACCEPTED" ->
                CallbackResult.Outcome.SUCCEEDED;
            case "FAILED", "FAILURE", "DECLINED", "REJECTED", "CANCELLED", "EXPIRED", "TIMEOUT" ->
                CallbackResult.Outcome.FAILED;
            default -> CallbackResult.Outcome.UNKNOWN;
        };
    }

    protected JsonNode readTree(byte[] body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.warn("[REAL {}] failed to parse callback body", provider(), e);
            return null;
        }
    }

    protected static String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    protected static BigDecimal decimal(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) return null;
        JsonNode v = node.get(field);
        try {
            return v.isNumber() ? v.decimalValue() : new BigDecimal(v.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

package com.nexuserp.payment.adapter.in.rest;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.domain.port.in.HandlePaymentCallbackUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint PUBLIC de réception des webhooks providers Mobile Money.
 *
 * OWASP — CHEMIN CRITIQUE :
 *  - A08/A02 : le corps est consommé BRUT (byte[]) et la signature HMAC-SHA256
 *    est vérifiée AVANT toute désérialisation ou mutation d'état.
 *  - Signature invalide => 401 et AUCUN changement d'état.
 *  - permitAll au niveau Spring Security (pas de JWT car appel serveur-à-serveur
 *    du provider) ; l'authenticité repose entièrement sur le HMAC.
 *  - Le path {provider} sélectionne la stratégie/secret ; valeur inconnue => 400.
 */
@RestController
@RequestMapping("/api/v1/payments/webhooks")
@Tag(name = "Payment Webhooks", description = "Callbacks providers Mobile Money (signature HMAC requise)")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final HandlePaymentCallbackUseCase handlePaymentCallbackUseCase;

    public PaymentWebhookController(HandlePaymentCallbackUseCase handlePaymentCallbackUseCase) {
        this.handlePaymentCallbackUseCase = handlePaymentCallbackUseCase;
    }

    @PostMapping("/{provider}")
    @Operation(summary = "Recevoir un callback provider",
        description = "Endpoint public ; authenticité garantie par signature HMAC-SHA256 du corps brut.")
    public ResponseEntity<Void> receive(
            @PathVariable("provider") String provider,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody(required = false) byte[] rawBody) {

        PaymentProvider providerEnum = parseProvider(provider);
        byte[] body = rawBody != null ? rawBody : new byte[0];

        try {
            handlePaymentCallbackUseCase.handleCallback(providerEnum, body, signature);
            return ResponseEntity.ok().build();
        } catch (DomainException ex) {
            if ("SIGNATURE_INVALID".equals(ex.getErrorCode())) {
                // A08 : pas de divulgation, pas de mutation, 401.
                log.warn("Webhook {} rejected: {}", providerEnum, ex.getErrorCode());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            if ("ENTITY_NOT_FOUND".equals(ex.getErrorCode())
                || "CALLBACK_UNPARSABLE".equals(ex.getErrorCode())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            throw ex;
        }
    }

    private PaymentProvider parseProvider(String raw) {
        try {
            return PaymentProvider.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw DomainException.of("PROVIDER_UNSUPPORTED", "Unknown provider: " + raw);
        }
    }
}

package com.nexuserp.notification.adapter.out.sms;

import com.nexuserp.notification.domain.model.SmsMessage;
import com.nexuserp.notification.domain.port.out.SendResult;
import com.nexuserp.notification.domain.port.out.SmsSenderPort;
import com.nexuserp.notification.domain.service.SmsTemplateResolver;
import com.nexuserp.notification.infrastructure.config.SmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Adaptateur HTTP générique pour agrégateurs SMS africains (CI / UEMOA).
 * Construit un payload JSON standard {to, from, message} et POST sur la baseUrl
 * avec un en-tête Authorization Bearer (clé API issue de la config/env).
 *
 * OWASP :
 *  - A03 : le corps texte est résolu/sanitizé par SmsTemplateResolver ; le numéro
 *          et le texte sont transmis comme champs JSON (jamais concaténés dans l'URL).
 *  - A09 : aucune clé API ni numéro complet n'est journalisé (numéro masqué).
 *  - Fail-safe : toute exception est convertie en SendResult.failed (jamais propagée).
 */
public class SmsGatewayAdapter implements SmsSenderPort {

    private static final Logger log = LoggerFactory.getLogger(SmsGatewayAdapter.class);

    private final SmsProperties.Sms config;
    private final SmsTemplateResolver resolver;
    private final RestClient restClient;

    public SmsGatewayAdapter(SmsProperties.Sms config, SmsTemplateResolver resolver, RestClient restClient) {
        this.config = config;
        this.resolver = resolver;
        this.restClient = restClient;
    }

    @Override
    public SendResult send(SmsMessage message) {
        String body = resolver.render(message.type(), message.locale(), message.variables());
        Map<String, Object> payload = Map.of(
            "to", message.recipientPhone(),
            "from", config.senderId() == null ? "NexusERP" : config.senderId(),
            "message", body
        );
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                .uri(config.baseUrl())
                .header("Authorization", "Bearer " + config.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

            String providerId = response == null ? null : String.valueOf(response.getOrDefault("messageId", response.get("id")));
            log.info("SMS dispatched via gateway: to={}, type={}, tenant={}",
                message.maskedPhone(), message.type(), message.tenantId());
            return SendResult.accepted(providerId);
        } catch (Exception e) {
            // Fail-safe : ne jamais propager — le consumer/dispatcher reste vivant.
            log.error("SMS gateway error: to={}, type={}, tenant={}, error={}",
                message.maskedPhone(), message.type(), message.tenantId(), e.getMessage());
            return SendResult.failed(e.getMessage());
        }
    }
}

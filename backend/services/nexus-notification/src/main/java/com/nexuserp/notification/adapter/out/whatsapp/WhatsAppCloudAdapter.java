package com.nexuserp.notification.adapter.out.whatsapp;

import com.nexuserp.notification.domain.model.SmsMessage;
import com.nexuserp.notification.domain.port.out.SendResult;
import com.nexuserp.notification.domain.port.out.WhatsAppSenderPort;
import com.nexuserp.notification.domain.service.SmsTemplateResolver;
import com.nexuserp.notification.infrastructure.config.SmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Adaptateur WhatsApp Business Cloud API (Meta Graph API).
 * POST {baseUrl}/{fromPhoneId}/messages avec un payload de type "text"
 * (le corps est résolu/sanitizé en amont). La structure suit la forme officielle
 * Cloud API ; on utilise ici le type texte pour rester provider-agnostic au runtime.
 *
 * OWASP :
 *  - A03 : corps texte sanitizé (SmsTemplateResolver) ; valeurs passées en champs JSON.
 *  - A09 : token et numéro complet jamais journalisés (numéro masqué).
 *  - Fail-safe : exceptions converties en SendResult.failed.
 */
public class WhatsAppCloudAdapter implements WhatsAppSenderPort {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudAdapter.class);

    private final SmsProperties.WhatsApp config;
    private final SmsTemplateResolver resolver;
    private final RestClient restClient;

    public WhatsAppCloudAdapter(SmsProperties.WhatsApp config, SmsTemplateResolver resolver, RestClient restClient) {
        this.config = config;
        this.resolver = resolver;
        this.restClient = restClient;
    }

    @Override
    public SendResult sendTemplate(SmsMessage message) {
        String body = resolver.render(message.type(), message.locale(), message.variables());
        String uri = config.baseUrl() + "/" + config.fromPhoneId() + "/messages";
        Map<String, Object> payload = Map.of(
            "messaging_product", "whatsapp",
            "recipient_type", "individual",
            "to", message.recipientPhone(),
            "type", "text",
            "text", Map.of("preview_url", false, "body", body)
        );
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                .uri(uri)
                .header("Authorization", "Bearer " + config.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

            String providerId = extractMessageId(response);
            log.info("WhatsApp dispatched via Cloud API: to={}, type={}, tenant={}",
                message.maskedPhone(), message.type(), message.tenantId());
            return SendResult.accepted(providerId);
        } catch (Exception e) {
            log.error("WhatsApp Cloud API error: to={}, type={}, tenant={}, error={}",
                message.maskedPhone(), message.type(), message.tenantId(), e.getMessage());
            return SendResult.failed(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map<String, Object> response) {
        if (response == null) return null;
        Object messages = response.get("messages");
        if (messages instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object id = first.get("id");
            return id == null ? null : String.valueOf(id);
        }
        return null;
    }
}

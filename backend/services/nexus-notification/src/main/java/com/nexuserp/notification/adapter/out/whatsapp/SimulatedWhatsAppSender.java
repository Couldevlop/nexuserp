package com.nexuserp.notification.adapter.out.whatsapp;

import com.nexuserp.notification.domain.model.SmsMessage;
import com.nexuserp.notification.domain.port.out.SendResult;
import com.nexuserp.notification.domain.port.out.WhatsAppSenderPort;
import com.nexuserp.notification.domain.service.SmsTemplateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Implémentation simulée du port WhatsApp — active sans credential (dev).
 * OWASP A09 : numéro masqué, pas de corps sensible journalisé.
 */
public class SimulatedWhatsAppSender implements WhatsAppSenderPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedWhatsAppSender.class);

    private final SmsTemplateResolver resolver;

    public SimulatedWhatsAppSender(SmsTemplateResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public SendResult sendTemplate(SmsMessage message) {
        String body = resolver.render(message.type(), message.locale(), message.variables());
        String simulatedId = "sim-wa-" + UUID.randomUUID();
        log.info("[SIMULATED WHATSAPP] to={}, type={}, tenant={}, length={}, simulatedId={}",
            message.maskedPhone(), message.type(), message.tenantId(), body.length(), simulatedId);
        return SendResult.accepted(simulatedId);
    }
}

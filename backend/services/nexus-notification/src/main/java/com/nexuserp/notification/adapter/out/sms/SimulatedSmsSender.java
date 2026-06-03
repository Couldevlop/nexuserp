package com.nexuserp.notification.adapter.out.sms;

import com.nexuserp.notification.domain.model.SmsMessage;
import com.nexuserp.notification.domain.port.out.SendResult;
import com.nexuserp.notification.domain.port.out.SmsSenderPort;
import com.nexuserp.notification.domain.service.SmsTemplateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Implémentation simulée du port SMS — utilisée quand aucune credential n'est fournie.
 * Permet de développer / tester sans dépendre d'un fournisseur réel.
 *
 * OWASP A09 : on ne journalise jamais le numéro complet (masqué) ni le corps
 * pour les types sensibles (2FA / reset) afin d'éviter la fuite de codes OTP.
 */
public class SimulatedSmsSender implements SmsSenderPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedSmsSender.class);

    private final SmsTemplateResolver resolver;

    public SimulatedSmsSender(SmsTemplateResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public SendResult send(SmsMessage message) {
        String body = resolver.render(message.type(), message.locale(), message.variables());
        String simulatedId = "sim-sms-" + UUID.randomUUID();
        log.info("[SIMULATED SMS] to={}, type={}, tenant={}, length={}, simulatedId={}",
            message.maskedPhone(), message.type(), message.tenantId(), body.length(), simulatedId);
        return SendResult.accepted(simulatedId);
    }
}

package com.nexuserp.notification.domain.port.out;

import com.nexuserp.notification.domain.model.SmsMessage;

/**
 * Port sortant pour l'envoi de messages WhatsApp Business (templates).
 * Implémenté par un adaptateur WhatsApp Cloud API ou par un simulateur.
 */
public interface WhatsAppSenderPort {

    /**
     * Envoie un message template WhatsApp. Le corps texte est résolu en amont
     * (SmsTemplateResolver) et passé comme paramètre du template.
     */
    SendResult sendTemplate(SmsMessage message);
}

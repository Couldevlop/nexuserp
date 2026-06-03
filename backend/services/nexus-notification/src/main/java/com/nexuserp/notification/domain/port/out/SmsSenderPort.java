package com.nexuserp.notification.domain.port.out;

import com.nexuserp.notification.domain.model.SmsMessage;

/**
 * Port sortant (hexagonal) pour l'envoi de SMS.
 * Implémenté par un adaptateur HTTP générique (agrégateurs africains)
 * ou par un simulateur en l'absence de credentials.
 */
public interface SmsSenderPort {

    SendResult send(SmsMessage message);
}

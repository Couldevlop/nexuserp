package com.nexuserp.notification.domain.service;

import com.nexuserp.notification.domain.model.NotificationChannel;
import com.nexuserp.notification.domain.model.NotificationMessage;
import com.nexuserp.notification.domain.model.SmsMessage;
import com.nexuserp.notification.domain.port.out.SendResult;
import com.nexuserp.notification.domain.port.out.SmsSenderPort;
import com.nexuserp.notification.domain.port.out.WhatsAppSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service de domaine : route une notification vers le(s) bon(s) canal/canaux.
 * - EMAIL    -> NotificationService (existant, inchangé)
 * - SMS      -> SmsSenderPort
 * - WHATSAPP -> WhatsAppSenderPort
 *
 * Fail-safe (OWASP) : une erreur d'un fournisseur n'interrompt pas le traitement
 * des autres canaux ni le consumer Kafka — chaque canal est encapsulé en try/catch.
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationService emailService;
    private final SmsSenderPort smsSender;
    private final WhatsAppSenderPort whatsAppSender;

    public NotificationDispatcher(NotificationService emailService,
                                  SmsSenderPort smsSender,
                                  WhatsAppSenderPort whatsAppSender) {
        this.emailService = emailService;
        this.smsSender = smsSender;
        this.whatsAppSender = whatsAppSender;
    }

    public void dispatch(NotificationMessage message) {
        for (NotificationChannel channel : message.effectiveChannels()) {
            try {
                switch (channel) {
                    case EMAIL -> dispatchEmail(message);
                    case SMS -> dispatchSms(message);
                    case WHATSAPP -> dispatchWhatsApp(message);
                    case PUSH -> log.debug("PUSH channel handled elsewhere (FCM), skipping in dispatcher");
                }
            } catch (Exception e) {
                // Fail-safe : on n'interrompt jamais les autres canaux.
                log.error("Channel dispatch failed: channel={}, type={}, tenant={}, error={}",
                    channel, message.type(), message.tenantId(), e.getMessage(), e);
            }
        }
    }

    private void dispatchEmail(NotificationMessage message) {
        if (message.recipientEmail() == null || message.recipientEmail().isBlank()) {
            log.warn("EMAIL channel requested but no recipientEmail: type={}, tenant={}",
                message.type(), message.tenantId());
            return;
        }
        emailService.send(message);
    }

    private void dispatchSms(NotificationMessage message) {
        SmsMessage sms = toSms(message);
        if (sms == null) return;
        SendResult result = smsSender.send(sms);
        logResult("SMS", message, sms.maskedPhone(), result);
    }

    private void dispatchWhatsApp(NotificationMessage message) {
        SmsMessage sms = toSms(message);
        if (sms == null) return;
        SendResult result = whatsAppSender.sendTemplate(sms);
        logResult("WHATSAPP", message, sms.maskedPhone(), result);
    }

    private SmsMessage toSms(NotificationMessage message) {
        if (message.recipientPhone() == null || message.recipientPhone().isBlank()) {
            log.warn("SMS/WhatsApp channel requested but no recipientPhone: type={}, tenant={}",
                message.type(), message.tenantId());
            return null;
        }
        return new SmsMessage(
            message.tenantId(),
            message.recipientPhone(),
            message.recipientName(),
            message.type(),
            message.locale(),
            message.variables()
        );
    }

    private void logResult(String channel, NotificationMessage message, String maskedPhone, SendResult result) {
        if (result.accepted()) {
            log.info("{} sent: type={}, recipient={}, tenant={}, providerMessageId={}",
                channel, message.type(), maskedPhone, message.tenantId(), result.providerMessageId());
        } else {
            log.error("{} rejected: type={}, recipient={}, tenant={}, error={}",
                channel, message.type(), maskedPhone, message.tenantId(), result.error());
        }
    }
}

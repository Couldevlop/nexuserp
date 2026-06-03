package com.nexuserp.notification.domain.service;

import com.nexuserp.notification.domain.model.NotificationChannel;
import com.nexuserp.notification.domain.model.NotificationMessage;
import com.nexuserp.notification.domain.model.NotificationMessage.NotificationType;
import com.nexuserp.notification.domain.model.SmsMessage;
import com.nexuserp.notification.domain.port.out.SendResult;
import com.nexuserp.notification.domain.port.out.SmsSenderPort;
import com.nexuserp.notification.domain.port.out.WhatsAppSenderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock private NotificationService emailService;
    @Mock private SmsSenderPort smsSender;
    @Mock private WhatsAppSenderPort whatsAppSender;
    @InjectMocks private NotificationDispatcher dispatcher;

    @Test
    @DisplayName("Should route to email service only for EMAIL channel")
    void shouldRouteToEmailOnly() {
        NotificationMessage msg = new NotificationMessage("t1", "a@b.com", "Bob",
            NotificationType.PAYMENT_RECEIVED, "fr-FR", Map.of(), null,
            List.of(NotificationChannel.EMAIL));

        dispatcher.dispatch(msg);

        verify(emailService).send(msg);
        verifyNoInteractions(smsSender, whatsAppSender);
    }

    @Test
    @DisplayName("Should route to SMS port for SMS channel")
    void shouldRouteToSms() {
        when(smsSender.send(any())).thenReturn(SendResult.accepted("sms-1"));
        NotificationMessage msg = new NotificationMessage("t1", null, "Bob",
            NotificationType.TWO_FA_CODE, "fr-FR", Map.of("code", "111"), "+2250700000000",
            List.of(NotificationChannel.SMS));

        dispatcher.dispatch(msg);

        verify(smsSender).send(any(SmsMessage.class));
        verifyNoInteractions(emailService, whatsAppSender);
    }

    @Test
    @DisplayName("Should route to WhatsApp port for WHATSAPP channel")
    void shouldRouteToWhatsApp() {
        when(whatsAppSender.sendTemplate(any())).thenReturn(SendResult.accepted("wa-1"));
        NotificationMessage msg = new NotificationMessage("t1", null, "Bob",
            NotificationType.LOW_STOCK_ALERT, "fr-FR", Map.of(), "+2250700000000",
            List.of(NotificationChannel.WHATSAPP));

        dispatcher.dispatch(msg);

        verify(whatsAppSender).sendTemplate(any(SmsMessage.class));
        verifyNoInteractions(emailService, smsSender);
    }

    @Test
    @DisplayName("Should route to both email and SMS for multi-channel message")
    void shouldRouteToMultipleChannels() {
        when(smsSender.send(any())).thenReturn(SendResult.accepted("sms-1"));
        NotificationMessage msg = new NotificationMessage("t1", "a@b.com", "Bob",
            NotificationType.INVOICE_DUE_REMINDER, "fr-FR", Map.of(), "+2250700000000",
            List.of(NotificationChannel.EMAIL, NotificationChannel.SMS));

        dispatcher.dispatch(msg);

        verify(emailService).send(msg);
        verify(smsSender).send(any(SmsMessage.class));
    }

    @Test
    @DisplayName("Should skip SMS when no phone provided")
    void shouldSkipSmsWhenNoPhone() {
        NotificationMessage msg = new NotificationMessage("t1", null, "Bob",
            NotificationType.TWO_FA_CODE, "fr-FR", Map.of(), null,
            List.of(NotificationChannel.SMS));

        dispatcher.dispatch(msg);

        verifyNoInteractions(smsSender, emailService, whatsAppSender);
    }

    @Test
    @DisplayName("Should not propagate provider exception (fail-safe)")
    void shouldNotPropagateProviderException() {
        when(smsSender.send(any())).thenThrow(new RuntimeException("provider down"));
        NotificationMessage msg = new NotificationMessage("t1", "a@b.com", "Bob",
            NotificationType.TWO_FA_CODE, "fr-FR", Map.of(), "+2250700000000",
            List.of(NotificationChannel.SMS, NotificationChannel.EMAIL));

        dispatcher.dispatch(msg);

        // Malgré l'échec SMS, l'email doit quand même être tenté.
        verify(emailService).send(msg);
    }

    @Test
    @DisplayName("Should fall back to EMAIL when no channels specified (backward compatible)")
    void shouldFallBackToEmail() {
        NotificationMessage msg = new NotificationMessage("t1", "a@b.com", "Bob",
            NotificationType.PAYMENT_RECEIVED, "fr-FR", Map.of());

        dispatcher.dispatch(msg);

        verify(emailService).send(msg);
    }
}

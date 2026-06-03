package com.nexuserp.notification.adapter.out.sms;

import com.nexuserp.notification.domain.model.NotificationMessage.NotificationType;
import com.nexuserp.notification.domain.model.SmsMessage;
import com.nexuserp.notification.domain.port.out.SendResult;
import com.nexuserp.notification.domain.service.SmsTemplateResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedSmsSenderTest {

    private final SimulatedSmsSender sender = new SimulatedSmsSender(new SmsTemplateResolver());

    @Test
    @DisplayName("Should accept message and return simulated provider id")
    void shouldAcceptAndReturnSimulatedId() {
        SmsMessage msg = new SmsMessage("t1", "+2250700000000", "Bob",
            NotificationType.TWO_FA_CODE, "fr-FR", Map.of("code", "123456"));

        SendResult result = sender.send(msg);

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).startsWith("sim-sms-");
        assertThat(result.error()).isNull();
    }

    @Test
    @DisplayName("Should mask phone number for logging (A09)")
    void shouldMaskPhone() {
        SmsMessage msg = new SmsMessage("t1", "+2250700000089", "Bob",
            NotificationType.PAYMENT_RECEIVED, "fr-FR", Map.of("amount", "100"));

        String masked = msg.maskedPhone();

        assertThat(masked).startsWith("+225").endsWith("89").contains("*");
        assertThat(masked).doesNotContain("0700000000");
    }
}

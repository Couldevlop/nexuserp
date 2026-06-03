package com.nexuserp.notification.infrastructure.config;

import com.nexuserp.notification.adapter.out.sms.SimulatedSmsSender;
import com.nexuserp.notification.adapter.out.sms.SmsGatewayAdapter;
import com.nexuserp.notification.adapter.out.whatsapp.SimulatedWhatsAppSender;
import com.nexuserp.notification.adapter.out.whatsapp.WhatsAppCloudAdapter;
import com.nexuserp.notification.domain.port.out.SmsSenderPort;
import com.nexuserp.notification.domain.port.out.WhatsAppSenderPort;
import com.nexuserp.notification.domain.service.SmsTemplateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Sélection de l'implémentation des ports SMS / WhatsApp.
 *
 * Switch simulé vs réel : décidé au démarrage selon la présence de credentials.
 * - credentials présents -> adaptateur HTTP réel (prod / staging)
 * - credentials absents   -> simulateur (dev, zéro credential requis)
 *
 * Ce switch par code (plutôt que par profil) garantit qu'un déploiement
 * sans secret ne tente jamais d'appel sortant.
 */
@Configuration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsChannelConfig {

    private static final Logger log = LoggerFactory.getLogger(SmsChannelConfig.class);

    @Bean
    public RestClient smsRestClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public SmsSenderPort smsSenderPort(SmsProperties props,
                                       SmsTemplateResolver resolver,
                                       RestClient smsRestClient) {
        if (props.sms().hasCredentials()) {
            log.info("SMS channel: real gateway enabled (provider={})", props.sms().provider());
            return new SmsGatewayAdapter(props.sms(), resolver, smsRestClient);
        }
        log.warn("SMS channel: no credentials -> SimulatedSmsSender (dev mode, no outbound calls)");
        return new SimulatedSmsSender(resolver);
    }

    @Bean
    public WhatsAppSenderPort whatsAppSenderPort(SmsProperties props,
                                                 SmsTemplateResolver resolver,
                                                 RestClient smsRestClient) {
        if (props.whatsapp().hasCredentials()) {
            log.info("WhatsApp channel: real Cloud API enabled");
            return new WhatsAppCloudAdapter(props.whatsapp(), resolver, smsRestClient);
        }
        log.warn("WhatsApp channel: no credentials -> SimulatedWhatsAppSender (dev mode, no outbound calls)");
        return new SimulatedWhatsAppSender(resolver);
    }
}

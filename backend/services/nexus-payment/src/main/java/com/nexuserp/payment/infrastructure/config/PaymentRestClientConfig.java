package com.nexuserp.payment.infrastructure.config;

import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Fournit un {@link RestClient.Builder} partagé pour les appels aux API réelles des
 * providers Mobile Money.
 *
 * RestClient (synchrone) provient de {@code spring-boot-starter-web} — aucune
 * dépendance supplémentaire requise. Les timeouts sont bornés pour éviter de bloquer
 * les Virtual Threads sur un provider lent (résilience / A05 misconfiguration).
 */
@Configuration
public class PaymentRestClientConfig {

    @Bean
    public ClientHttpRequestFactory paymentProviderRequestFactory() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofSeconds(5))
            .withReadTimeout(Duration.ofSeconds(20));
        return ClientHttpRequestFactories.get(settings);
    }

    @Bean
    public RestClient.Builder paymentProviderRestClientBuilder(ClientHttpRequestFactory factory) {
        return RestClient.builder().requestFactory(factory);
    }
}

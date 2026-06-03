package com.nexuserp.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * nexus-payment — Service de collecte & réconciliation Mobile Money (Orange Money,
 * Wave, MTN MoMo, Moov). Différenciateur africain de NexusERP.
 *
 * scanBasePackages inclut com.nexuserp.core pour réutiliser TenantContext,
 * TenantInterceptor, GlobalExceptionHandler et les value objects partagés.
 */
@SpringBootApplication(scanBasePackages = {"com.nexuserp.payment", "com.nexuserp.core"})
@ConfigurationPropertiesScan("com.nexuserp.payment")
@EnableCaching
public class NexusPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusPaymentApplication.class, args);
    }
}

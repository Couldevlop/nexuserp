package com.nexuserp.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * nexus-config — Magasin centralisé, multi-tenant et chiffré de configuration/paramètres.
 *
 * Objectif : clés API & paramètres ajoutés/édités depuis l'UI admin, secrets chiffrés au repos,
 * lus par les autres services (ex. nexus-payment) via l'endpoint interne — "ajouter la clé
 * dans l'UI admin -> elle s'active", sans changement de code.
 *
 * scanBasePackages inclut com.nexuserp.core pour réutiliser TenantContext, TenantInterceptor,
 * GlobalExceptionHandler, DomainEvent et les value objects partagés (cohérent avec nexus-payment).
 */
@SpringBootApplication(scanBasePackages = {"com.nexuserp.config", "com.nexuserp.core"})
@ConfigurationPropertiesScan("com.nexuserp.config")
public class NexusConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusConfigApplication.class, args);
    }
}

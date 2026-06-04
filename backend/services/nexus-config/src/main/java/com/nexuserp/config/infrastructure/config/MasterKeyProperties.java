package com.nexuserp.config.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clé maître de chiffrement des secrets.
 *
 * OWASP A02 : la clé provient EXCLUSIVEMENT de l'environnement (Vault / K8s Secret)
 * via NEXUS_CONFIG_MASTER_KEY, encodée en base64 (32 octets = AES-256).
 * En développement, si absente, une clé dérivée DEV-only est utilisée (avec WARNING).
 */
@ConfigurationProperties(prefix = "nexus.config.encryption")
public record MasterKeyProperties(
    String masterKey
) {}

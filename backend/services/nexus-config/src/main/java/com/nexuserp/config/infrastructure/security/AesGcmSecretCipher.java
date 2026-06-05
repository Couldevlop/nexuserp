package com.nexuserp.config.infrastructure.security;

import com.nexuserp.config.domain.port.out.SecretCipher;
import com.nexuserp.config.infrastructure.config.MasterKeyProperties;
import com.nexuserp.core.domain.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Chiffrement AES-256-GCM des valeurs secrètes (OWASP A02 — Cryptographic Failures).
 *
 * Format de sortie : base64( IV(12) || ciphertext || tag(16) ).
 *  - GCM produit ciphertext+tag ensemble via {@code Cipher.doFinal} ; on préfixe l'IV.
 *  - IV aléatoire de 12 octets régénéré à CHAQUE chiffrement (jamais réutilisé).
 *
 * Clé maître :
 *  - PROD : NEXUS_CONFIG_MASTER_KEY (base64, 32 octets) — obligatoire hors dev.
 *  - DEV  : si absente, une clé dérivée (SHA-256 d'une constante) est utilisée, avec WARNING.
 *           Cette clé NE DOIT JAMAIS servir en production.
 */
@Component
public class AesGcmSecretCipher implements SecretCipher {

    private static final Logger log = LoggerFactory.getLogger(AesGcmSecretCipher.class);

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    private static final int IV_LENGTH = 12;          // 96 bits — recommandé pour GCM
    private static final int TAG_LENGTH_BITS = 128;   // 16 octets
    private static final int KEY_LENGTH_BYTES = 32;   // AES-256

    // Graine DEV-only — n'a aucune valeur de sécurité, sert uniquement à faire tourner le dev.
    private static final String DEV_SEED = "nexus-config-DEV-ONLY-master-key-seed";

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmSecretCipher(MasterKeyProperties properties, Environment environment) {
        this.keySpec = resolveKey(properties, environment);
    }

    private SecretKeySpec resolveKey(MasterKeyProperties properties, Environment environment) {
        String configured = properties != null ? properties.masterKey() : null;
        boolean prod = isProdProfile(environment);

        if (configured != null && !configured.isBlank()) {
            byte[] key = decodeKey(configured);
            return new SecretKeySpec(key, ALGORITHM);
        }

        if (prod) {
            // A02 — refus de démarrer en prod sans clé maître explicite.
            throw DomainException.of("CONFIG_MASTER_KEY_MISSING",
                "NEXUS_CONFIG_MASTER_KEY is required in production (base64, 32 bytes)");
        }

        log.warn("⚠ NEXUS_CONFIG_MASTER_KEY not set — using a DERIVED DEV-ONLY key. "
            + "NEVER use this in staging/production. Set NEXUS_CONFIG_MASTER_KEY (base64 32 bytes).");
        byte[] derived = sha256(DEV_SEED.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(derived, ALGORITHM);
    }

    private static boolean isProdProfile(Environment environment) {
        if (environment == null) {
            return false;
        }
        for (String p : environment.getActiveProfiles()) {
            if (p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production") || p.equalsIgnoreCase("staging")) {
                return true;
            }
        }
        return false;
    }

    private static byte[] decodeKey(String base64) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw DomainException.of("CONFIG_MASTER_KEY_INVALID", "master key must be valid base64");
        }
        if (key.length != KEY_LENGTH_BYTES) {
            throw DomainException.of("CONFIG_MASTER_KEY_INVALID",
                "master key must decode to exactly 32 bytes (AES-256), got " + key.length);
        }
        return key;
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Override
    public String encrypt(String plain) {
        if (plain == null) {
            throw DomainException.of("CONFIG_ENCRYPT_ERROR", "plain value is null");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv); // IV aléatoire unique par valeur

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherTextWithTag = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + cipherTextWithTag.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherTextWithTag, 0, out, iv.length, cipherTextWithTag.length);

            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            // Ne jamais inclure la valeur en clair dans le message d'erreur (A09).
            throw DomainException.of("CONFIG_ENCRYPT_ERROR", "failed to encrypt secret value");
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            throw DomainException.of("CONFIG_DECRYPT_ERROR", "cipher text is null");
        }
        try {
            byte[] all = Base64.getDecoder().decode(cipherText);
            if (all.length <= IV_LENGTH) {
                throw DomainException.of("CONFIG_DECRYPT_ERROR", "cipher text too short");
            }
            byte[] iv = Arrays.copyOfRange(all, 0, IV_LENGTH);
            byte[] cipherTextWithTag = Arrays.copyOfRange(all, IV_LENGTH, all.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(cipherTextWithTag); // lève si le tag GCM ne valide pas (tampering)

            return new String(plain, StandardCharsets.UTF_8);
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            // Tampering, mauvaise clé, format invalide -> échec sans détail (A02/A09).
            throw DomainException.of("CONFIG_DECRYPT_ERROR", "failed to decrypt secret value (tampered or wrong key)");
        }
    }
}

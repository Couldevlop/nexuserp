package com.nexuserp.config.infrastructure.security;

import com.nexuserp.config.infrastructure.config.MasterKeyProperties;
import com.nexuserp.core.domain.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AesGcmSecretCipher — AES-256-GCM round-trip & tampering")
class AesGcmSecretCipherTest {

    private AesGcmSecretCipher cipher;

    @BeforeEach
    void setUp() {
        // Clé maître de test : 32 octets base64.
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) i;
        }
        String base64Key = Base64.getEncoder().encodeToString(key);
        cipher = new AesGcmSecretCipher(new MasterKeyProperties(base64Key), new MockEnvironment());
    }

    @Test
    @DisplayName("Should encrypt then decrypt to the original plaintext")
    void shouldRoundTrip() {
        String plain = "sk_live_super-secret-wave-api-key-123";

        String encrypted = cipher.encrypt(plain);

        assertThat(encrypted).isNotEqualTo(plain);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(plain);
    }

    @Test
    @DisplayName("Should produce a different ciphertext (different IV) for the same plaintext")
    void shouldUseDifferentIvEachTime() {
        String plain = "same-value";

        String first = cipher.encrypt(plain);
        String second = cipher.encrypt(plain);

        assertThat(first).isNotEqualTo(second);
        // Mais les deux déchiffrent vers la même valeur.
        assertThat(cipher.decrypt(first)).isEqualTo(plain);
        assertThat(cipher.decrypt(second)).isEqualTo(plain);
    }

    @Test
    @DisplayName("Should fail to decrypt when ciphertext is tampered (GCM auth tag)")
    void shouldFailOnTampering() {
        String encrypted = cipher.encrypt("integrity-protected");
        byte[] raw = Base64.getDecoder().decode(encrypted);
        // Altérer un octet du corps (après l'IV de 12 octets).
        raw[raw.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> cipher.decrypt(tampered))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("decrypt");
    }

    @Test
    @DisplayName("Should reject a master key that is not 32 bytes")
    void shouldRejectInvalidKeyLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() ->
            new AesGcmSecretCipher(new MasterKeyProperties(shortKey), new MockEnvironment()))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("Should use a DEV-only derived key when master key is absent in dev profile")
    void shouldDeriveDevKey_whenAbsentInDev() {
        AesGcmSecretCipher devCipher =
            new AesGcmSecretCipher(new MasterKeyProperties(null), new MockEnvironment());
        String encrypted = devCipher.encrypt("dev-value");
        assertThat(devCipher.decrypt(encrypted)).isEqualTo("dev-value");
    }

    @Test
    @DisplayName("Should refuse to start in production without a master key")
    void shouldFail_whenAbsentInProd() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        assertThatThrownBy(() ->
            new AesGcmSecretCipher(new MasterKeyProperties(null), prod))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("required in production");
    }
}

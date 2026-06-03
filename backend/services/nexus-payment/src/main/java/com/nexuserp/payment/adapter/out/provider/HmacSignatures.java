package com.nexuserp.payment.adapter.out.provider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Utilitaires HMAC-SHA256 pour la vérification des webhooks providers.
 *
 * OWASP A02 (Cryptographic Failures) / A08 (Integrity Failures) :
 *  - HMAC-SHA256 du corps HTTP brut.
 *  - Comparaison en TEMPS CONSTANT via {@link MessageDigest#isEqual(byte[], byte[])}
 *    pour éviter les attaques par timing.
 */
public final class HmacSignatures {

    private HmacSignatures() {}

    public static String hmacSha256Hex(byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HMAC-SHA256", e);
        }
    }

    /**
     * Vérifie la signature en temps constant. Accepte un header optionnellement
     * préfixé "sha256=" (convention courante chez les providers).
     */
    public static boolean isValid(byte[] body, String signatureHeader, String secret) {
        if (signatureHeader == null || signatureHeader.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        String provided = signatureHeader.startsWith("sha256=")
            ? signatureHeader.substring("sha256=".length())
            : signatureHeader;
        String expected = hmacSha256Hex(body, secret);
        // Comparaison en temps constant.
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            provided.getBytes(StandardCharsets.UTF_8));
    }
}

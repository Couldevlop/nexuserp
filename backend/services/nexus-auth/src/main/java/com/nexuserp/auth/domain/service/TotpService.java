package com.nexuserp.auth.domain.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Service TOTP — Génération et vérification des codes OTP (Google Authenticator compatible).
 */
@Service
public class TotpService {

    private static final String RECOVERY_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RECOVERY_CODE_LENGTH = 8;
    private static final int RECOVERY_CODE_COUNT = 10;

    private final GoogleAuthenticator gAuth;
    private final String issuer;

    public TotpService(@Value("${nexuserp.2fa.totp.issuer:NexusERP}") String issuer) {
        this.gAuth = new GoogleAuthenticator();
        this.issuer = issuer;
    }

    /**
     * Génère une nouvelle clé secrète TOTP.
     */
    public String generateSecret() {
        GoogleAuthenticatorKey credentials = gAuth.createCredentials();
        return credentials.getKey();
    }

    /**
     * Génère l'URL pour le QR code (format otpauth://).
     */
    public String generateQrCodeUrl(String secret, String userEmail, String tenantId) {
        String accountName = userEmail + " (" + tenantId + ")";
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(issuer, accountName,
            new GoogleAuthenticatorKey.Builder(secret).build());
    }

    /**
     * Vérifie un code TOTP avec fenêtre de tolérance ±1 période.
     */
    public boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }

    /**
     * Génère des codes de récupération one-time.
     */
    public List<String> generateRecoveryCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>(RECOVERY_CODE_COUNT);
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            StringBuilder sb = new StringBuilder(RECOVERY_CODE_LENGTH);
            for (int j = 0; j < RECOVERY_CODE_LENGTH; j++) {
                sb.append(RECOVERY_CHARSET.charAt(random.nextInt(RECOVERY_CHARSET.length())));
            }
            codes.add(sb.toString());
        }
        return codes;
    }
}

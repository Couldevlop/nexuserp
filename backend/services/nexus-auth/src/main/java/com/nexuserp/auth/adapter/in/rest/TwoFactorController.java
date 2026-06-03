package com.nexuserp.auth.adapter.in.rest;

import com.nexuserp.auth.domain.model.UserTwoFactor;
import com.nexuserp.auth.domain.repository.UserTwoFactorRepository;
import com.nexuserp.auth.domain.service.TotpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller 2FA — Setup TOTP, vérification, désactivation, codes de récupération.
 */
@RestController
@RequestMapping("/api/v1/auth/2fa")
@Tag(name = "2FA", description = "Authentification à deux facteurs")
public class TwoFactorController {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorController.class);

    private final TotpService totpService;
    private final UserTwoFactorRepository twoFactorRepository;

    public TwoFactorController(TotpService totpService, UserTwoFactorRepository twoFactorRepository) {
        this.totpService = totpService;
        this.twoFactorRepository = twoFactorRepository;
    }

    /**
     * POST /setup — Génère le secret TOTP et l'URL QR code.
     */
    @PostMapping("/setup")
    @Transactional
    @Operation(summary = "Démarrer la configuration TOTP")
    public ResponseEntity<TotpSetupResponse> setupTotp(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String tenantId = jwt.getClaimAsString("tenant_id");
        String email = jwt.getClaimAsString("email");

        // Supprimer setup précédent si existant
        twoFactorRepository.findByUserIdAndTenantId(userId, tenantId)
            .ifPresent(existing -> {
                if (!existing.isActive()) {
                    twoFactorRepository.delete(existing);
                }
            });

        String secret = totpService.generateSecret();
        String qrUrl = totpService.generateQrCodeUrl(secret, email, tenantId);

        UserTwoFactor record = UserTwoFactor.createTotpSetup(userId, tenantId, secret);
        twoFactorRepository.save(record);

        log.info("TOTP setup initiated for user={}, tenant={}", userId, tenantId);
        return ResponseEntity.ok(new TotpSetupResponse(secret, qrUrl));
    }

    /**
     * POST /verify — Vérifie le code TOTP et active le 2FA.
     */
    @PostMapping("/verify")
    @Transactional
    @Operation(summary = "Vérifier et activer le TOTP")
    public ResponseEntity<TotpVerifyResponse> verifyAndActivate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TotpVerifyRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String tenantId = jwt.getClaimAsString("tenant_id");

        UserTwoFactor record = twoFactorRepository.findByUserIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> new IllegalStateException("2FA setup not initiated"));

        if (!totpService.verifyCode(record.getTotpSecret(), request.code())) {
            return ResponseEntity.badRequest().body(
                new TotpVerifyResponse(false, null, "Invalid TOTP code"));
        }

        List<String> recoveryCodes = totpService.generateRecoveryCodes();
        record.activate(String.join(",", recoveryCodes));
        twoFactorRepository.save(record);

        log.info("TOTP activated for user={}, tenant={}", userId, tenantId);
        return ResponseEntity.ok(new TotpVerifyResponse(true, recoveryCodes, null));
    }

    /**
     * POST /validate — Valide un code TOTP pour une session en cours.
     */
    @PostMapping("/validate")
    @Transactional
    @Operation(summary = "Valider un code TOTP")
    public ResponseEntity<Map<String, Boolean>> validateCode(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TotpVerifyRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String tenantId = jwt.getClaimAsString("tenant_id");

        UserTwoFactor record = twoFactorRepository.findByUserIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> new IllegalStateException("2FA not configured"));

        if (!record.isActive()) {
            return ResponseEntity.badRequest().build();
        }

        boolean valid = totpService.verifyCode(record.getTotpSecret(), request.code());
        if (valid) {
            record.recordUsage();
            twoFactorRepository.save(record);
        }

        return ResponseEntity.ok(Map.of("valid", valid));
    }

    /**
     * GET /status — Statut 2FA de l'utilisateur courant.
     */
    @GetMapping("/status")
    @Transactional(readOnly = true)
    @Operation(summary = "Statut 2FA")
    public ResponseEntity<TwoFactorStatusResponse> getStatus(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String tenantId = jwt.getClaimAsString("tenant_id");

        return twoFactorRepository.findByUserIdAndTenantId(userId, tenantId)
            .map(r -> ResponseEntity.ok(new TwoFactorStatusResponse(
                r.isActive(), r.getMethod().name(), r.getEnabledAt())))
            .orElse(ResponseEntity.ok(new TwoFactorStatusResponse(false, null, null)));
    }

    /**
     * POST /disable — Désactive le 2FA (requiert code de confirmation).
     */
    @PostMapping("/disable")
    @Transactional
    @Operation(summary = "Désactiver le 2FA")
    public ResponseEntity<Map<String, String>> disable(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TotpVerifyRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String tenantId = jwt.getClaimAsString("tenant_id");

        UserTwoFactor record = twoFactorRepository.findByUserIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> new IllegalStateException("2FA not configured"));

        if (!totpService.verifyCode(record.getTotpSecret(), request.code())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid TOTP code"));
        }

        record.disable();
        twoFactorRepository.save(record);

        log.warn("2FA disabled for user={}, tenant={}", userId, tenantId);
        return ResponseEntity.ok(Map.of("status", "2FA disabled"));
    }

    // ─── DTOs ───────────────────────────────────────────────────────────────

    public record TotpSetupResponse(String secret, String qrCodeUrl) {}

    public record TotpVerifyRequest(@NotNull Integer code) {}

    public record TotpVerifyResponse(boolean success, List<String> recoveryCodes, String error) {}

    public record TwoFactorStatusResponse(boolean enabled, String method, java.time.Instant enabledAt) {}
}

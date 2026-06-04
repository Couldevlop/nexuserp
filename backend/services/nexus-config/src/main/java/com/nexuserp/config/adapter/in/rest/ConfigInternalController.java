package com.nexuserp.config.adapter.in.rest;

import com.nexuserp.config.domain.port.in.ResolveSecretUseCase;
import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.core.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API INTERNE — résolution de valeurs DÉCHIFFRÉES pour les services consommateurs
 * (ex. nexus-payment qui lit ses clés API ici). C'est le point d'appel du ConfigClient.
 *
 * OWASP :
 *  - A01 : restreint au rôle SERVICE (service-à-service), + admins. Scoppé par tenant.
 *  - A09 : CHAQUE accès est audité (clé + appelant), JAMAIS la valeur.
 */
@RestController
@RequestMapping("/api/v1/config/internal")
@Tag(name = "Config Internal", description = "Résolution de valeurs déchiffrées — service-à-service uniquement")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('SERVICE','TENANT_ADMIN','SUPER_ADMIN')")
public class ConfigInternalController {

    private static final Logger auditLog = LoggerFactory.getLogger("nexus.config.audit");

    private final ResolveSecretUseCase resolveSecretUseCase;

    public ConfigInternalController(ResolveSecretUseCase resolveSecretUseCase) {
        this.resolveSecretUseCase = resolveSecretUseCase;
    }

    @GetMapping("/{key}/value")
    @Operation(summary = "Résoudre la valeur déchiffrée d'un paramètre (interne)",
        description = "Retourne la valeur en clair. Réservé aux services (rôle SERVICE). Chaque accès est audité.")
    public ResponseEntity<ResolvedValueDto> resolve(@PathVariable String key) {
        String tenantId = TenantContext.getTenantId();
        String caller = currentCaller();

        // A09 — audit d'accès : clé + appelant, jamais la valeur.
        auditLog.info("SECRET_ACCESS key={} tenant={} caller={}", key, tenantId, caller);

        String value = resolveSecretUseCase.resolve(tenantId, key)
            .orElseThrow(() -> DomainException.notFound("ConfigParameter", key));

        return ResponseEntity.ok(new ResolvedValueDto(key, value));
    }

    private String currentCaller() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }
}

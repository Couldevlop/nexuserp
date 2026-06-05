package com.nexuserp.config.adapter.in.rest;

import com.nexuserp.config.application.command.UpsertConfigCommand;
import com.nexuserp.config.application.query.ConfigQuery;
import com.nexuserp.config.domain.model.ConfigCategory;
import com.nexuserp.config.domain.model.ConfigView;
import com.nexuserp.config.domain.port.in.DeleteConfigUseCase;
import com.nexuserp.config.domain.port.in.GetConfigUseCase;
import com.nexuserp.config.domain.port.in.UpsertConfigUseCase;
import com.nexuserp.core.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API d'administration de la configuration (UI admin).
 *
 * A01 (Broken Access Control) :
 *  - réservée à TENANT_ADMIN / SUPER_ADMIN (@PreAuthorize),
 *  - toutes les opérations sont scoppées via TenantContext.
 * A02/A09 : les secrets sont TOUJOURS masqués en réponse (jamais de clair).
 */
@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Config Admin", description = "Magasin centralisé de paramètres & clés API (secrets chiffrés)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
public class ConfigAdminController {

    private final UpsertConfigUseCase upsertConfigUseCase;
    private final GetConfigUseCase getConfigUseCase;
    private final DeleteConfigUseCase deleteConfigUseCase;

    public ConfigAdminController(UpsertConfigUseCase upsertConfigUseCase,
                                 GetConfigUseCase getConfigUseCase,
                                 DeleteConfigUseCase deleteConfigUseCase) {
        this.upsertConfigUseCase = upsertConfigUseCase;
        this.getConfigUseCase = getConfigUseCase;
        this.deleteConfigUseCase = deleteConfigUseCase;
    }

    @GetMapping
    @Operation(summary = "Lister les paramètres (secrets masqués)",
        description = "Filtre optionnel: category. Les valeurs secrètes sont remplacées par un masque.")
    public ResponseEntity<List<ConfigDto>> list(@RequestParam(required = false) String category) {
        ConfigCategory cat = category != null && !category.isBlank()
            ? ConfigCategory.valueOf(category.toUpperCase())
            : null;
        ConfigQuery query = new ConfigQuery(TenantContext.getTenantId(), cat);
        List<ConfigDto> result = getConfigUseCase.list(query).stream().map(ConfigDto::from).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{key}")
    @Operation(summary = "Obtenir un paramètre par clé (secret masqué)")
    public ResponseEntity<ConfigDto> getByKey(@PathVariable String key) {
        ConfigView view = getConfigUseCase.getByKey(TenantContext.getTenantId(), key);
        return ResponseEntity.ok(ConfigDto.from(view));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Créer ou mettre à jour un paramètre",
        description = "Upsert idempotent par clé. Si secret=true (ou type=SECRET), la valeur est chiffrée au repos. "
            + "Publie un événement nexus.config.changed pour invalider les caches consommateurs.")
    public ResponseEntity<ConfigDto> upsert(@PathVariable String key,
                                            @Valid @RequestBody UpsertConfigRequest request) {
        UpsertConfigCommand command = new UpsertConfigCommand(
            TenantContext.getTenantId(),
            key,
            request.value(),
            request.type(),
            request.category(),
            request.secret(),
            request.description(),
            TenantContext.getUserId());
        ConfigView view = upsertConfigUseCase.upsert(command);
        return ResponseEntity.ok(ConfigDto.from(view));
    }

    @DeleteMapping("/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un paramètre")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        deleteConfigUseCase.delete(TenantContext.getTenantId(), key, TenantContext.getUserId());
        return ResponseEntity.noContent().build();
    }
}

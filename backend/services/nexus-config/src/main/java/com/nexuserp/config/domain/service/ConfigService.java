package com.nexuserp.config.domain.service;

import com.nexuserp.config.application.command.UpsertConfigCommand;
import com.nexuserp.config.application.query.ConfigQuery;
import com.nexuserp.config.domain.model.ConfigParameter;
import com.nexuserp.config.domain.model.ConfigValueType;
import com.nexuserp.config.domain.model.ConfigView;
import com.nexuserp.config.domain.port.in.DeleteConfigUseCase;
import com.nexuserp.config.domain.port.in.GetConfigUseCase;
import com.nexuserp.config.domain.port.in.ResolveSecretUseCase;
import com.nexuserp.config.domain.port.in.UpsertConfigUseCase;
import com.nexuserp.config.domain.port.out.ConfigEventPublisher;
import com.nexuserp.config.domain.port.out.ConfigRepository;
import com.nexuserp.config.domain.port.out.SecretCipher;
import com.nexuserp.core.domain.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service domaine Config — implémente les Use Cases du magasin de configuration.
 * La logique métier réside ici (CLAUDE.md §17), pas dans les contrôleurs ni l'entité JPA.
 *
 * OWASP :
 *  - A02 : les secrets sont chiffrés (SecretCipher) AVANT persistance ; jamais retournés
 *    en clair par les lectures admin (masquage via ConfigView).
 *  - A01 : toute opération est scoppée par tenantId.
 *  - A09 : journalisation de la clé/catégorie uniquement, jamais de la valeur.
 */
@Service
@Transactional
public class ConfigService implements UpsertConfigUseCase, GetConfigUseCase, DeleteConfigUseCase, ResolveSecretUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    private final ConfigRepository repository;
    private final ConfigEventPublisher eventPublisher;
    private final SecretCipher secretCipher;

    public ConfigService(ConfigRepository repository,
                         ConfigEventPublisher eventPublisher,
                         SecretCipher secretCipher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.secretCipher = secretCipher;
    }

    // ─── Upsert ─────────────────────────────────────────────────────────────────

    @Override
    public ConfigView upsert(UpsertConfigCommand cmd) {
        boolean secret = cmd.secret() || cmd.valueType() == ConfigValueType.SECRET;

        // A03 — validation de cohérence valeur/type avant toute persistance.
        if (!secret) {
            cmd.valueType().validate(cmd.value());
        }

        // A02 — chiffrement des secrets au repos ; valeur claire stockée telle quelle sinon.
        String storedValue = storedValueFor(secret, cmd.value());

        ConfigParameter param = repository.findByTenantIdAndKey(cmd.tenantId(), cmd.key())
            .map(existing -> {
                existing.update(cmd.category(), cmd.valueType(), secret, storedValue,
                    cmd.description(), cmd.updatedBy());
                return existing;
            })
            .orElseGet(() -> {
                ConfigParameter created = ConfigParameter.builder()
                    .tenantId(cmd.tenantId())
                    .key(cmd.key())
                    .category(cmd.category())
                    .valueType(cmd.valueType())
                    .secret(secret)
                    .storedValue(storedValue)
                    .description(cmd.description())
                    .updatedBy(cmd.updatedBy())
                    .build();
                // Émet l'événement UPSERTED pour la création.
                created.update(cmd.category(), cmd.valueType(), secret, storedValue,
                    cmd.description(), cmd.updatedBy());
                return created;
            });

        ConfigParameter saved = repository.save(param);
        List<Object> events = List.copyOf(saved.getDomainEvents());
        saved.clearDomainEvents();
        eventPublisher.publishAll(events);

        log.info("Config upserted key={}, category={}, secret={}, tenant={}, by={}",
            saved.getKey(), saved.getCategory(), saved.isSecret(), cmd.tenantId(), cmd.updatedBy());

        return ConfigView.masked(saved);
    }

    private String storedValueFor(boolean secret, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return secret ? secretCipher.encrypt(value) : value;
    }

    // ─── Queries (masquées) ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ConfigView> list(ConfigQuery query) {
        List<ConfigParameter> params = query.category() != null
            ? repository.findByTenantIdAndCategory(query.tenantId(), query.category())
            : repository.findByTenantId(query.tenantId());
        return params.stream().map(ConfigView::masked).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigView getByKey(String tenantId, String key) {
        return repository.findByTenantIdAndKey(tenantId, key)
            .map(ConfigView::masked)
            .orElseThrow(() -> DomainException.notFound("ConfigParameter", key));
    }

    // ─── Resolve (déchiffré — usage interne) ─────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<String> resolve(String tenantId, String key) {
        return repository.findByTenantIdAndKey(tenantId, key)
            .filter(ConfigParameter::hasValue)
            .map(p -> p.isSecret() ? secretCipher.decrypt(p.getStoredValue()) : p.getStoredValue());
    }

    // ─── Delete ─────────────────────────────────────────────────────────────────

    @Override
    public void delete(String tenantId, String key, String deletedBy) {
        ConfigParameter param = repository.findByTenantIdAndKey(tenantId, key)
            .orElseThrow(() -> DomainException.notFound("ConfigParameter", key));

        boolean removed = repository.deleteByTenantIdAndKey(tenantId, key);
        if (!removed) {
            throw DomainException.notFound("ConfigParameter", key);
        }
        param.markDeleted(deletedBy);
        List<Object> events = List.copyOf(param.getDomainEvents());
        param.clearDomainEvents();
        eventPublisher.publishAll(events);

        log.info("Config deleted key={}, tenant={}, by={}", key, tenantId, deletedBy);
    }
}

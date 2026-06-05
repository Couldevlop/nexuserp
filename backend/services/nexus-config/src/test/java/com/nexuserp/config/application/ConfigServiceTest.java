package com.nexuserp.config.application;

import com.nexuserp.config.application.command.UpsertConfigCommand;
import com.nexuserp.config.application.query.ConfigQuery;
import com.nexuserp.config.domain.event.ConfigChangedEvent;
import com.nexuserp.config.domain.model.ConfigCategory;
import com.nexuserp.config.domain.model.ConfigParameter;
import com.nexuserp.config.domain.model.ConfigValueType;
import com.nexuserp.config.domain.model.ConfigView;
import com.nexuserp.config.domain.port.out.ConfigEventPublisher;
import com.nexuserp.config.domain.port.out.ConfigRepository;
import com.nexuserp.config.domain.port.out.SecretCipher;
import com.nexuserp.config.domain.service.ConfigService;
import com.nexuserp.core.domain.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigService — Use Case Tests")
class ConfigServiceTest {

    @Mock private ConfigRepository repository;
    @Mock private ConfigEventPublisher eventPublisher;
    @Mock private SecretCipher secretCipher;

    private ConfigService service;

    private static final String TENANT = "ci-acme";

    @BeforeEach
    void setUp() {
        service = new ConfigService(repository, eventPublisher, secretCipher);
    }

    // ─── Upsert ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should encrypt secret, mask response and publish UPSERTED event")
    void shouldEncryptSecret_andPublish() {
        when(repository.findByTenantIdAndKey(TENANT, "payment.wave.apiKey")).thenReturn(Optional.empty());
        when(secretCipher.encrypt("sk_live_123")).thenReturn("ENC(sk_live_123)");
        when(repository.save(any(ConfigParameter.class))).thenAnswer(i -> i.getArgument(0));

        UpsertConfigCommand cmd = new UpsertConfigCommand(
            TENANT, "payment.wave.apiKey", "sk_live_123",
            ConfigValueType.SECRET, ConfigCategory.PAYMENT, true, "Wave API key", "admin-1");

        ConfigView view = service.upsert(cmd);

        // La valeur secrète est chiffrée avant persistance.
        verify(secretCipher).encrypt("sk_live_123");
        ArgumentCaptor<ConfigParameter> captor = ArgumentCaptor.forClass(ConfigParameter.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStoredValue()).isEqualTo("ENC(sk_live_123)");
        assertThat(captor.getValue().isSecret()).isTrue();

        // La réponse est masquée (jamais le clair ni le chiffré).
        assertThat(view.secret()).isTrue();
        assertThat(view.set()).isTrue();
        assertThat(view.value()).isEqualTo(ConfigView.MASK);

        // Un événement nexus.config.changed UPSERTED est publié.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object>> events = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publishAll(events.capture());
        assertThat(events.getValue()).hasSize(1);
        ConfigChangedEvent evt = (ConfigChangedEvent) events.getValue().get(0);
        assertThat(evt.getKey()).isEqualTo("payment.wave.apiKey");
        assertThat(evt.getAction()).isEqualTo(ConfigChangedEvent.Action.UPSERTED);
        assertThat(evt.isSecret()).isTrue();
    }

    @Test
    @DisplayName("Should store non-secret value as-is (no encryption)")
    void shouldStorePlain_whenNotSecret() {
        when(repository.findByTenantIdAndKey(TENANT, "general.companyName")).thenReturn(Optional.empty());
        when(repository.save(any(ConfigParameter.class))).thenAnswer(i -> i.getArgument(0));

        UpsertConfigCommand cmd = new UpsertConfigCommand(
            TENANT, "general.companyName", "ACME CI",
            ConfigValueType.STRING, ConfigCategory.GENERAL, false, "Company name", "admin-1");

        ConfigView view = service.upsert(cmd);

        verify(secretCipher, never()).encrypt(any());
        assertThat(view.secret()).isFalse();
        assertThat(view.value()).isEqualTo("ACME CI");
    }

    @Test
    @DisplayName("Should reject invalid NUMBER value (A03)")
    void shouldThrow_whenInvalidNumber() {
        UpsertConfigCommand cmd = new UpsertConfigCommand(
            TENANT, "tax.vatRate", "not-a-number",
            ConfigValueType.NUMBER, ConfigCategory.TAX, false, null, "admin-1");

        assertThatThrownBy(() -> service.upsert(cmd))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("NUMBER");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should update existing parameter and publish event")
    void shouldUpdateExisting() {
        ConfigParameter existing = ConfigParameter.builder()
            .tenantId(TENANT).key("payment.wave.apiKey")
            .category(ConfigCategory.PAYMENT).valueType(ConfigValueType.SECRET).secret(true)
            .storedValue("ENC(old)").updatedBy("admin-0").build();
        when(repository.findByTenantIdAndKey(TENANT, "payment.wave.apiKey")).thenReturn(Optional.of(existing));
        when(secretCipher.encrypt("new-key")).thenReturn("ENC(new-key)");
        when(repository.save(any(ConfigParameter.class))).thenAnswer(i -> i.getArgument(0));

        UpsertConfigCommand cmd = new UpsertConfigCommand(
            TENANT, "payment.wave.apiKey", "new-key",
            ConfigValueType.SECRET, ConfigCategory.PAYMENT, true, "rotated", "admin-2");

        ConfigView view = service.upsert(cmd);

        assertThat(existing.getStoredValue()).isEqualTo("ENC(new-key)");
        assertThat(view.value()).isEqualTo(ConfigView.MASK);
        verify(eventPublisher).publishAll(anyList());
    }

    // ─── Get / List (masked) ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should mask secret on getByKey")
    void shouldMaskSecret_onGet() {
        ConfigParameter secret = ConfigParameter.builder()
            .tenantId(TENANT).key("payment.wave.apiKey")
            .category(ConfigCategory.PAYMENT).valueType(ConfigValueType.SECRET).secret(true)
            .storedValue("ENC(sk_live_123)").updatedBy("admin-1").build();
        when(repository.findByTenantIdAndKey(TENANT, "payment.wave.apiKey")).thenReturn(Optional.of(secret));

        ConfigView view = service.getByKey(TENANT, "payment.wave.apiKey");

        assertThat(view.value()).isEqualTo(ConfigView.MASK);
        assertThat(view.value()).doesNotContain("sk_live_123");
        assertThat(view.set()).isTrue();
    }

    @Test
    @DisplayName("Should list by category with secrets masked")
    void shouldListByCategory_masked() {
        ConfigParameter secret = ConfigParameter.builder()
            .tenantId(TENANT).key("payment.wave.apiKey")
            .category(ConfigCategory.PAYMENT).valueType(ConfigValueType.SECRET).secret(true)
            .storedValue("ENC(x)").updatedBy("admin-1").build();
        when(repository.findByTenantIdAndCategory(TENANT, ConfigCategory.PAYMENT))
            .thenReturn(List.of(secret));

        List<ConfigView> result = service.list(new ConfigQuery(TENANT, ConfigCategory.PAYMENT));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).value()).isEqualTo(ConfigView.MASK);
    }

    @Test
    @DisplayName("Should throw when getByKey not found")
    void shouldThrow_whenNotFound() {
        when(repository.findByTenantIdAndKey(TENANT, "missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByKey(TENANT, "missing"))
            .isInstanceOf(DomainException.class);
    }

    // ─── Resolve (decrypted) ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should decrypt secret value on resolve")
    void shouldDecrypt_onResolve() {
        ConfigParameter secret = ConfigParameter.builder()
            .tenantId(TENANT).key("payment.wave.apiKey")
            .category(ConfigCategory.PAYMENT).valueType(ConfigValueType.SECRET).secret(true)
            .storedValue("ENC(sk_live_123)").updatedBy("admin-1").build();
        when(repository.findByTenantIdAndKey(TENANT, "payment.wave.apiKey")).thenReturn(Optional.of(secret));
        when(secretCipher.decrypt("ENC(sk_live_123)")).thenReturn("sk_live_123");

        Optional<String> resolved = service.resolve(TENANT, "payment.wave.apiKey");

        assertThat(resolved).contains("sk_live_123");
        verify(secretCipher).decrypt("ENC(sk_live_123)");
    }

    @Test
    @DisplayName("Should return plain value without decryption when not secret")
    void shouldReturnPlain_onResolveNonSecret() {
        ConfigParameter plain = ConfigParameter.builder()
            .tenantId(TENANT).key("general.companyName")
            .category(ConfigCategory.GENERAL).valueType(ConfigValueType.STRING).secret(false)
            .storedValue("ACME CI").updatedBy("admin-1").build();
        when(repository.findByTenantIdAndKey(TENANT, "general.companyName")).thenReturn(Optional.of(plain));

        Optional<String> resolved = service.resolve(TENANT, "general.companyName");

        assertThat(resolved).contains("ACME CI");
        verify(secretCipher, never()).decrypt(any());
    }

    @Test
    @DisplayName("Should return empty on resolve when key absent")
    void shouldReturnEmpty_onResolveMissing() {
        when(repository.findByTenantIdAndKey(TENANT, "missing")).thenReturn(Optional.empty());
        assertThat(service.resolve(TENANT, "missing")).isEmpty();
    }

    // ─── Delete ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete existing parameter and publish DELETED event")
    void shouldDelete_andPublish() {
        ConfigParameter existing = ConfigParameter.builder()
            .tenantId(TENANT).key("payment.wave.apiKey")
            .category(ConfigCategory.PAYMENT).valueType(ConfigValueType.SECRET).secret(true)
            .storedValue("ENC(x)").updatedBy("admin-1").build();
        when(repository.findByTenantIdAndKey(TENANT, "payment.wave.apiKey")).thenReturn(Optional.of(existing));
        when(repository.deleteByTenantIdAndKey(TENANT, "payment.wave.apiKey")).thenReturn(true);

        service.delete(TENANT, "payment.wave.apiKey", "admin-2");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object>> events = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publishAll(events.capture());
        ConfigChangedEvent evt = (ConfigChangedEvent) events.getValue().get(0);
        assertThat(evt.getAction()).isEqualTo(ConfigChangedEvent.Action.DELETED);
    }

    @Test
    @DisplayName("Should throw when deleting a missing parameter")
    void shouldThrow_whenDeleteMissing() {
        when(repository.findByTenantIdAndKey(eq(TENANT), eq("missing"))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(TENANT, "missing", "admin-2"))
            .isInstanceOf(DomainException.class);
        verify(eventPublisher, never()).publishAll(anyList());
    }
}

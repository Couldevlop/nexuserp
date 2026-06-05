package com.nexuserp.payment.infrastructure.config;

import com.nexuserp.core.config.ConfigClient;
import com.nexuserp.core.infrastructure.tenant.TenantContext;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties.ProviderConfig;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties.RealApiConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie l'overlay store central (nexus-config) > variables d'environnement :
 *  - valeur store présente  -> prioritaire sur l'env,
 *  - valeur store absente   -> fallback env,
 *  - pas de tenant (webhook) ou pas de ConfigClient -> env seul,
 *  - mapping MTN : payment.mtn.api-key -> champ apiUserKey.
 */
@DisplayName("ProviderConfigResolver — overlay store central / env")
class ProviderConfigResolverTest {

    private static final String TENANT = "tenant-test";

    /** Faux ConfigClient en mémoire (store central simulé). */
    private static final class InMemoryConfigClient implements ConfigClient {
        private final Map<String, String> store = new HashMap<>();

        void put(String key, String value) {
            store.put(TENANT + "::" + key, value);
        }

        @Override
        public Optional<String> getValue(String tenantId, String key) {
            return Optional.ofNullable(store.get(tenantId + "::" + key));
        }

        @Override
        public void invalidate(String tenantId, String key) { /* no-op */ }

        @Override
        public void invalidateTenant(String tenantId) { /* no-op */ }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private RealApiConfig emptyReal() {
        return new RealApiConfig(null, null, null, null, null, null, null, null,
            null, null, null, null, null);
    }

    private PaymentProviderProperties propsWith(String providerKey, ProviderConfig cfg) {
        return new PaymentProviderProperties("http://cb", Map.of(providerKey, cfg));
    }

    @Test
    @DisplayName("Store value overrides env value (Wave api-key)")
    void shouldPreferStoreValue_whenDefined() {
        TenantContext.setTenantId(TENANT);
        RealApiConfig envReal = new RealApiConfig(null, null, null, null, null,
            "env-api-key", null, null, null, null, null, null, null);
        PaymentProviderProperties props =
            propsWith("wave", new ProviderConfig("env-secret", "https://api", "k", true, envReal));

        InMemoryConfigClient store = new InMemoryConfigClient();
        store.put("payment.wave.api-key", "store-api-key");
        ProviderConfigResolver resolver = new ProviderConfigResolver(props, store);

        ProviderConfig effective = resolver.forProvider("WAVE");
        assertThat(effective.real().apiKey()).isEqualTo("store-api-key");
        // Champ non défini dans le store -> fallback env.
        assertThat(effective.webhookSecret()).isEqualTo("env-secret");
    }

    @Test
    @DisplayName("Store activates a provider with NO env credentials (zero-code)")
    void shouldActivateProvider_whenStoreOnly() {
        TenantContext.setTenantId(TENANT);
        PaymentProviderProperties props =
            propsWith("wave", new ProviderConfig("s", "https://api", "k", true, emptyReal()));

        InMemoryConfigClient store = new InMemoryConfigClient();
        store.put("payment.wave.api-key", "ui-provided-key");
        ProviderConfigResolver resolver = new ProviderConfigResolver(props, store);

        ProviderConfig effective = resolver.forProvider("WAVE");
        assertThat(effective.isRealConfigured("WAVE")).isTrue();
    }

    @Test
    @DisplayName("MTN: payment.mtn.api-key maps to apiUserKey (not generic apiKey)")
    void shouldMapMtnApiKey_toApiUserKey() {
        TenantContext.setTenantId(TENANT);
        PaymentProviderProperties props =
            propsWith("mtn_momo", new ProviderConfig("s", "https://api", "k", true, emptyReal()));

        InMemoryConfigClient store = new InMemoryConfigClient();
        store.put("payment.mtn.api-user", "user-1");
        store.put("payment.mtn.api-key", "user-key-1");
        store.put("payment.mtn.subscription-key", "sub-1");
        ProviderConfigResolver resolver = new ProviderConfigResolver(props, store);

        ProviderConfig effective = resolver.forProvider("MTN_MOMO");
        assertThat(effective.real().apiUserKey()).isEqualTo("user-key-1");
        assertThat(effective.real().apiKey()).isNull();
        assertThat(effective.isRealConfigured("MTN_MOMO")).isTrue();
    }

    @Test
    @DisplayName("No TenantContext (incoming webhook) -> env config untouched")
    void shouldFallbackToEnv_whenNoTenant() {
        RealApiConfig envReal = new RealApiConfig(null, null, null, null, null,
            "env-api-key", null, null, null, null, null, null, null);
        ProviderConfig envCfg = new ProviderConfig("env-secret", "https://api", "k", true, envReal);
        PaymentProviderProperties props = propsWith("wave", envCfg);

        InMemoryConfigClient store = new InMemoryConfigClient();
        store.put("payment.wave.api-key", "store-api-key");
        ProviderConfigResolver resolver = new ProviderConfigResolver(props, store);

        // Pas de TenantContext.setTenantId : la config env est renvoyée telle quelle.
        assertThat(resolver.forProvider("WAVE")).isSameAs(envCfg);
    }

    @Test
    @DisplayName("No ConfigClient (nexus.config.url empty) -> env config untouched")
    void shouldFallbackToEnv_whenNoConfigClient() {
        TenantContext.setTenantId(TENANT);
        ProviderConfig envCfg = new ProviderConfig("env-secret", "https://api", "k", true, emptyReal());
        PaymentProviderProperties props = propsWith("wave", envCfg);

        ProviderConfigResolver resolver = new ProviderConfigResolver(props, null);
        assertThat(resolver.forProvider("WAVE")).isSameAs(envCfg);
    }

    @Test
    @DisplayName("Blank store value is ignored -> env fallback")
    void shouldIgnoreBlankStoreValue() {
        TenantContext.setTenantId(TENANT);
        RealApiConfig envReal = new RealApiConfig(null, null, null, null, null,
            "env-api-key", null, null, null, null, null, null, null);
        PaymentProviderProperties props =
            propsWith("wave", new ProviderConfig("s", "https://api", "k", true, envReal));

        InMemoryConfigClient store = new InMemoryConfigClient();
        store.put("payment.wave.api-key", "   ");
        ProviderConfigResolver resolver = new ProviderConfigResolver(props, store);

        assertThat(resolver.forProvider("WAVE").real().apiKey()).isEqualTo("env-api-key");
    }

    @Test
    @DisplayName("Unknown provider -> env config untouched")
    void shouldReturnEnv_whenUnknownProvider() {
        TenantContext.setTenantId(TENANT);
        PaymentProviderProperties props =
            propsWith("wave", new ProviderConfig("s", "https://api", "k", true, emptyReal()));
        ProviderConfigResolver resolver =
            new ProviderConfigResolver(props, new InMemoryConfigClient());

        assertThat(resolver.forProvider("UNKNOWN_PROVIDER")).isNull();
    }
}

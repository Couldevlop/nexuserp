package com.nexuserp.payment.adapter.out.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties.ProviderConfig;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties.RealApiConfig;
import com.nexuserp.payment.infrastructure.config.ProviderConfigResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie la sélection RÉEL vs SIMULÉ du {@link ProviderStrategyResolver}.
 *  - identifiants présents  -> stratégie RÉELLE choisie,
 *  - identifiants absents    -> stratégie SIMULÉE (défaut).
 */
@DisplayName("ProviderStrategyResolver — sélection réel/simulé")
class ProviderStrategyResolverTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient.Builder restBuilder = RestClient.builder();

    private RealApiConfig emptyReal() {
        return new RealApiConfig(null, null, null, null, null, null, null, null,
            null, null, null, null, null);
    }

    private PaymentProviderProperties propsWith(Map<String, ProviderConfig> providers) {
        return new PaymentProviderProperties("http://cb", providers);
    }

    private ProviderConfig cfg(RealApiConfig real) {
        return new ProviderConfig("secret", "https://api", "key", true, real);
    }

    /** Resolver sans store central (ConfigClient null) : env/props uniquement. */
    private ProviderConfigResolver cfgResolver(PaymentProviderProperties props) {
        return new ProviderConfigResolver(props, null);
    }

    private List<ProviderStrategy> allStrategies(PaymentProviderProperties props) {
        ProviderConfigResolver cfg = cfgResolver(props);
        return List.of(
            new OrangeMoneySimulatedStrategy(mapper, props),
            new WaveSimulatedStrategy(mapper, props),
            new MtnMomoSimulatedStrategy(mapper, props),
            new MoovMoneySimulatedStrategy(mapper, props),
            new OrangeMoneyRealStrategy(mapper, cfg, restBuilder),
            new WaveRealStrategy(mapper, cfg, restBuilder),
            new MtnMomoRealStrategy(mapper, cfg, restBuilder),
            new MoovMoneyRealStrategy(mapper, cfg, restBuilder)
        );
    }

    @Test
    @DisplayName("Empty credentials -> SIMULATED strategy for every provider")
    void shouldUseSimulated_whenNoCredentials() {
        PaymentProviderProperties props = propsWith(Map.of(
            "orange_money", cfg(emptyReal()),
            "wave", cfg(emptyReal()),
            "mtn_momo", cfg(emptyReal()),
            "moov_money", cfg(emptyReal())
        ));
        ProviderStrategyResolver resolver = new ProviderStrategyResolver(allStrategies(props), cfgResolver(props));

        assertThat(resolver.resolve(PaymentProvider.ORANGE_MONEY))
            .isInstanceOf(OrangeMoneySimulatedStrategy.class);
        assertThat(resolver.resolve(PaymentProvider.WAVE))
            .isInstanceOf(WaveSimulatedStrategy.class);
        assertThat(resolver.resolve(PaymentProvider.MTN_MOMO))
            .isInstanceOf(MtnMomoSimulatedStrategy.class);
        assertThat(resolver.resolve(PaymentProvider.MOOV_MONEY))
            .isInstanceOf(MoovMoneySimulatedStrategy.class);
        assertThat(resolver.isRealConfigured(PaymentProvider.WAVE)).isFalse();
    }

    @Test
    @DisplayName("Wave with api-key -> REAL strategy chosen")
    void shouldUseReal_whenWaveApiKeyPresent() {
        RealApiConfig waveReal = new RealApiConfig("https://api.wave.com", null, null, null, null,
            "wave-secret-key", null, null, null, null, null, null, "XOF");
        PaymentProviderProperties props = propsWith(Map.of(
            "wave", cfg(waveReal),
            "orange_money", cfg(emptyReal())
        ));
        ProviderStrategyResolver resolver = new ProviderStrategyResolver(allStrategies(props), cfgResolver(props));

        assertThat(resolver.isRealConfigured(PaymentProvider.WAVE)).isTrue();
        assertThat(resolver.resolve(PaymentProvider.WAVE))
            .isInstanceOf(WaveRealStrategy.class);
        // Orange has no creds -> still simulated.
        assertThat(resolver.resolve(PaymentProvider.ORANGE_MONEY))
            .isInstanceOf(OrangeMoneySimulatedStrategy.class);
    }

    @Test
    @DisplayName("Orange requires client-id + client-secret + merchant-key together")
    void shouldStaySimulated_whenOrangePartialCredentials() {
        // Only client-id present -> NOT configured.
        RealApiConfig partial = new RealApiConfig("https://api.orange.com", null, "client-id",
            null, null, null, null, null, null, null, null, null, null);
        PaymentProviderProperties props = propsWith(Map.of("orange_money", cfg(partial)));
        ProviderStrategyResolver resolver = new ProviderStrategyResolver(allStrategies(props), cfgResolver(props));

        assertThat(resolver.isRealConfigured(PaymentProvider.ORANGE_MONEY)).isFalse();
        assertThat(resolver.resolve(PaymentProvider.ORANGE_MONEY))
            .isInstanceOf(OrangeMoneySimulatedStrategy.class);

        // Full credentials -> REAL.
        RealApiConfig full = new RealApiConfig("https://api.orange.com", null, "client-id",
            "client-secret", "merchant-key", null, null, null, null, null, null, null, null);
        PaymentProviderProperties props2 = propsWith(Map.of("orange_money", cfg(full)));
        ProviderStrategyResolver resolver2 = new ProviderStrategyResolver(allStrategies(props2), cfgResolver(props2));
        assertThat(resolver2.resolve(PaymentProvider.ORANGE_MONEY))
            .isInstanceOf(OrangeMoneyRealStrategy.class);
    }

    @Test
    @DisplayName("MTN requires api-user + api-user-key + subscription-key")
    void shouldUseReal_whenMtnFullyConfigured() {
        RealApiConfig mtnReal = new RealApiConfig("https://sandbox.momodeveloper.mtn.com", null,
            null, null, null, null, "api-user", "api-user-key", "sub-key", "sandbox", null, null, "EUR");
        PaymentProviderProperties props = propsWith(Map.of("mtn_momo", cfg(mtnReal)));
        ProviderStrategyResolver resolver = new ProviderStrategyResolver(allStrategies(props), cfgResolver(props));

        assertThat(resolver.resolve(PaymentProvider.MTN_MOMO))
            .isInstanceOf(MtnMomoRealStrategy.class);
    }
}

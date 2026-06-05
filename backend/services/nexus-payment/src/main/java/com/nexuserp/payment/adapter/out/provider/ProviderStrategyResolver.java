package com.nexuserp.payment.adapter.out.provider;

import com.nexuserp.core.domain.exception.DomainException;
import com.nexuserp.payment.domain.model.PaymentProvider;
import com.nexuserp.payment.infrastructure.config.PaymentProviderProperties;
import com.nexuserp.payment.infrastructure.config.ProviderConfigResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Sélectionne, à l'exécution, la {@link ProviderStrategy} RÉELLE ou SIMULÉE pour un
 * {@link PaymentProvider} donné.
 *
 * ACTIVATION ZÉRO-CODE :
 *  - Si les identifiants réels du provider sont configurés
 *    ({@code configResolver.forProvider(p).isRealConfigured(p)} == true)
 *    ET qu'une stratégie réelle existe -> la stratégie RÉELLE est utilisée.
 *  - Sinon -> la stratégie SIMULÉE (comportement par défaut, tous les tests existants
 *    restent verts car aucun identifiant réel n'est fourni en test/dev).
 *
 * La résolution se fait à CHAQUE appel, sur la config EFFECTIVE
 * ({@link ProviderConfigResolver} : store central nexus-config par tenant
 * PRIORITAIRE, env en fallback). Deux façons d'activer un provider réel :
 *  1. ajouter les clés dans l'UI admin Paramétrage -> actif en ~60s, sans redémarrage ;
 *  2. renseigner les variables d'environnement et redémarrer (fallback historique).
 */
@Component
public class ProviderStrategyResolver {

    private static final Logger log = LoggerFactory.getLogger(ProviderStrategyResolver.class);

    private final Map<PaymentProvider, ProviderStrategy> realStrategies =
        new EnumMap<>(PaymentProvider.class);
    private final Map<PaymentProvider, ProviderStrategy> simulatedStrategies =
        new EnumMap<>(PaymentProvider.class);
    private final ProviderConfigResolver configResolver;

    public ProviderStrategyResolver(List<ProviderStrategy> allStrategies,
                                    ProviderConfigResolver configResolver) {
        this.configResolver = configResolver;
        for (ProviderStrategy s : allStrategies) {
            if (s instanceof AbstractRealProviderStrategy) {
                realStrategies.put(s.provider(), s);
            } else {
                // AbstractSimulatedProviderStrategy et toute autre implémentation par défaut.
                simulatedStrategies.put(s.provider(), s);
            }
        }
    }

    /**
     * @return la stratégie à utiliser pour ce provider (réelle si configurée, sinon simulée).
     */
    public ProviderStrategy resolve(PaymentProvider provider) {
        if (isRealConfigured(provider)) {
            ProviderStrategy real = realStrategies.get(provider);
            if (real != null) {
                return real;
            }
            log.warn("[RESOLVER] {} marked configured but no real strategy found — using simulated",
                provider);
        }
        ProviderStrategy simulated = simulatedStrategies.get(provider);
        if (simulated != null) {
            return simulated;
        }
        // Aucune stratégie simulée : retomber sur la réelle si présente, sinon erreur.
        ProviderStrategy real = realStrategies.get(provider);
        if (real != null) {
            return real;
        }
        throw DomainException.of("PROVIDER_UNSUPPORTED", "No gateway strategy for provider " + provider);
    }

    /** True si le provider doit utiliser son API réelle (identifiants présents). */
    public boolean isRealConfigured(PaymentProvider provider) {
        PaymentProviderProperties.ProviderConfig cfg = configResolver.forProvider(provider.name());
        return cfg != null && cfg.isRealConfigured(provider.name());
    }
}

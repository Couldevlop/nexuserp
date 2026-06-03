package com.nexuserp.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Rate limiting par tenant : 100 req/s par tenant, 1000 req/s global.
 * Implémentation via Redis avec fenêtre glissante simplifiée.
 */
@Component
public class RateLimitFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int TENANT_LIMIT_PER_SECOND = 100;
    private static final int GLOBAL_LIMIT_PER_SECOND = 1000;

    private final ReactiveRedisTemplate<String, Long> redisTemplate;

    public RateLimitFilter(ReactiveRedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        long epochSecond = System.currentTimeMillis() / 1000;
        
        String globalKey = "rate:global:" + epochSecond;

        // 1. Vérification Rate Limit GLOBAL
        return redisTemplate.opsForValue()
            .increment(globalKey)
            .flatMap(globalCount -> {
                // Si c'est la première requête de cette seconde, on met un TTL
                Mono<Boolean> expireGlobal = (globalCount == 1) 
                    ? redisTemplate.expire(globalKey, Duration.ofSeconds(2)) 
                    : Mono.just(true);

                return expireGlobal.then(Mono.just(globalCount));
            })
            .flatMap(globalCount -> {
                if (globalCount > GLOBAL_LIMIT_PER_SECOND) {
                    log.warn("Global rate limit exceeded: {}", globalCount);
                    return tooManyRequests(exchange);
                }

                // 2. Si pas de tenantId, on continue simplement
                if (tenantId == null) {
                    return chain.filter(exchange);
                }

                // 3. Vérification Rate Limit par TENANT
                String tenantKey = "rate:tenant:" + tenantId + ":" + epochSecond;
                return redisTemplate.opsForValue()
                    .increment(tenantKey)
                    .flatMap(tenantCount -> {
                        Mono<Boolean> expireTenant = (tenantCount == 1) 
                            ? redisTemplate.expire(tenantKey, Duration.ofSeconds(2)) 
                            : Mono.just(true);

                        return expireTenant.then(Mono.just(tenantCount));
                    })
                    .flatMap(tenantCount -> {
                        if (tenantCount > TENANT_LIMIT_PER_SECOND) {
                            log.warn("Tenant rate limit exceeded for tenant={}, count={}", tenantId, tenantCount);
                            return tooManyRequests(exchange);
                        }
                        
                        // Ajouter l'en-tête informatif sur les requêtes restantes
                        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", 
                            String.valueOf(Math.max(0, TENANT_LIMIT_PER_SECOND - tenantCount)));
                        
                        return chain.filter(exchange);
                    });
            })
            // Gestion des erreurs Redis (si Redis tombe, on laisse passer ou on bloque ?)
            .onErrorResume(e -> {
                log.error("Redis error during rate limiting: {}", e.getMessage());
                return chain.filter(exchange); // On laisse passer par défaut si Redis est HS
            });
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        exchange.getResponse().getHeaders().add("Retry-After", "1");
        return exchange.getResponse().setComplete();
    }
}
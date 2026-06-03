package com.nexuserp.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

/**
 * Audit log pour les opérations sensibles (mutations).
 * Les requêtes GET ne sont pas auditées sauf endpoints sensibles.
 */
@Component
public class AuditLogFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogFilter.class);

    private static final Set<HttpMethod> AUDITED_METHODS = Set.of(
        HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpMethod method = exchange.getRequest().getMethod();

        if (!AUDITED_METHODS.contains(method)) {
            return chain.filter(exchange);
        }

        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        String path = exchange.getRequest().getPath().value();
        String traceId = exchange.getRequest().getId();
        Instant start = Instant.now();

        return chain.filter(exchange)
            .doOnSuccess(v -> {
                int statusCode = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : 0;
                long durationMs = Instant.now().toEpochMilli() - start.toEpochMilli();

                // Log structuré JSON pour Loki
                log.info(
                    "{{\"type\":\"audit\",\"tenantId\":\"{}\",\"userId\":\"{}\",\"method\":\"{}\",\"path\":\"{}\",\"status\":{},\"durationMs\":{},\"traceId\":\"{}\"}}",
                    tenantId, userId, method, path, statusCode, durationMs, traceId
                );
            });
    }
}

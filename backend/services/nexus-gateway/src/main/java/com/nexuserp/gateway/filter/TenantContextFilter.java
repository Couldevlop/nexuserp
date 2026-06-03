package com.nexuserp.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extrait le tenantId depuis le JWT et l'injecte dans les headers downstream.
 * Le tenantId est stocké dans le claim "tenantId" du token Keycloak.
 */
@Component
public class TenantContextFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    public static final String TENANT_ID_HEADER = "X-Tenant-ID";
    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> {
                var auth = ctx.getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                    String tenantId = jwt.getClaimAsString("tenantId");
                    String userId = jwt.getSubject();

                    if (tenantId == null || tenantId.isBlank()) {
                        log.warn("JWT missing tenantId claim for user={}", userId);
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }

                    ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(req -> req
                            .header(TENANT_ID_HEADER, tenantId)
                            .header(USER_ID_HEADER, userId)
                        )
                        .build();

                    return chain.filter(mutatedExchange);
                }
                return chain.filter(exchange);
            })
            .switchIfEmpty(chain.filter(exchange));
    }
}

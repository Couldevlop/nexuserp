package com.nexuserp.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import com.nexuserp.gateway.filter.TenantContextFilter;
import com.nexuserp.gateway.filter.RateLimitFilter;
import com.nexuserp.gateway.filter.AuditLogFilter;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator nexusRouteLocator(
            RouteLocatorBuilder builder,
            TenantContextFilter tenantFilter,
            RateLimitFilter rateLimitFilter,
            AuditLogFilter auditFilter) {

        return builder.routes()

            // ─── nexus-auth ───────────────────────────────────────────────
            .route("nexus-auth", r -> r
                .path("/api/v1/auth/**")
                .filters(f -> f
                    .rewritePath("/api/v1/auth/(?<segment>.*)", "/api/v1/auth/${segment}")
                    .filter(rateLimitFilter)
                    .circuitBreaker(c -> c.setName("auth-cb").setFallbackUri("forward:/fallback/auth")))
                .uri("lb://nexus-auth"))

            // ─── nexus-finance ────────────────────────────────────────────
            .route("nexus-finance", r -> r
                .path("/api/v1/finance/**")
                .filters(f -> f
                    .rewritePath("/api/v1/finance/(?<segment>.*)", "/api/v1/finance/${segment}")
                    .filter(tenantFilter)
                    .filter(rateLimitFilter)
                    .filter(auditFilter)
                    .circuitBreaker(c -> c.setName("finance-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://nexus-finance"))

            // ─── nexus-procurement ────────────────────────────────────────
            .route("nexus-procurement", r -> r
                .path("/api/v1/procurement/**")
                .filters(f -> f
                    .filter(tenantFilter)
                    .filter(rateLimitFilter)
                    .filter(auditFilter)
                    .circuitBreaker(c -> c.setName("procurement-cb")))
                .uri("lb://nexus-procurement"))

            // ─── nexus-inventory ──────────────────────────────────────────
            .route("nexus-inventory", r -> r
                .path("/api/v1/inventory/**")
                .filters(f -> f
                    .filter(tenantFilter)
                    .filter(rateLimitFilter))
                .uri("lb://nexus-inventory"))

            // ─── nexus-sales ──────────────────────────────────────────────
            .route("nexus-sales", r -> r
                .path("/api/v1/sales/**")
                .filters(f -> f
                    .filter(tenantFilter)
                    .filter(rateLimitFilter)
                    .filter(auditFilter))
                .uri("lb://nexus-sales"))

            // ─── nexus-hr ─────────────────────────────────────────────────
            .route("nexus-hr", r -> r
                .path("/api/v1/hr/**")
                .filters(f -> f
                    .filter(tenantFilter)
                    .filter(rateLimitFilter))
                .uri("lb://nexus-hr"))

            // ─── nexus-production ─────────────────────────────────────────
            .route("nexus-production", r -> r
                .path("/api/v1/production/**")
                .filters(f -> f
                    .filter(tenantFilter)
                    .filter(rateLimitFilter))
                .uri("lb://nexus-production"))

            // ─── nexus-ai ─────────────────────────────────────────────────
            .route("nexus-ai", r -> r
                .path("/api/v1/ai/**")
                .filters(f -> f
                    .rewritePath("/api/v1/ai/(?<segment>.*)", "/ai/v1/${segment}")
                    .filter(tenantFilter)
                    .filter(rateLimitFilter)
                    .circuitBreaker(c -> c.setName("ai-cb").setFallbackUri("forward:/fallback/ai")))
                .uri("lb://nexus-ai"))

            // ─── nexus-reporting ──────────────────────────────────────────
            .route("nexus-reporting", r -> r
                .path("/api/v1/reporting/**")
                .filters(f -> f
                    .filter(tenantFilter)
                    .filter(rateLimitFilter))
                .uri("lb://nexus-reporting"))

            // ─── nexus-import ─────────────────────────────────────────────
            .route("nexus-import", r -> r
                .path("/api/v1/import/**")
                .filters(f -> f
                    .filter(tenantFilter)
                    .filter(rateLimitFilter))
                .uri("lb://nexus-import"))

            // ─── nexus-notification ───────────────────────────────────────
            .route("nexus-notification", r -> r
                .path("/api/v1/notifications/**")
                .filters(f -> f
                    .filter(tenantFilter)
                    .filter(rateLimitFilter))
                .uri("lb://nexus-notification"))

            // ─── nexus-payment (Mobile Money) ─────────────────────────────
            .route("nexus-payment", r -> r
                .path("/api/v1/payments/**")
                .filters(f -> f
                    .filter(tenantFilter)
                    .filter(rateLimitFilter)
                    .filter(auditFilter))
                .uri("lb://nexus-payment"))

            // ─── nexus-config (paramétrage centralisé chiffré) ────────────
            // NB : /api/v1/config/internal/** (valeurs déchiffrées) exige le
            // rôle SERVICE côté nexus-config — pas accessible aux users.
            .route("nexus-config", r -> r
                .path("/api/v1/config/**")
                .filters(f -> f
                    .filter(tenantFilter)
                    .filter(rateLimitFilter)
                    .filter(auditFilter))
                .uri("lb://nexus-config"))

            .build();
    }
}

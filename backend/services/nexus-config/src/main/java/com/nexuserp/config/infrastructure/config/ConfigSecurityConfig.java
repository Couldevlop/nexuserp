package com.nexuserp.config.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sécurité nexus-config — OAuth2 Resource Server (JWT Keycloak), STATELESS.
 *
 * OWASP :
 *  - A01 : tout est authentifié par défaut sauf actuator/swagger.
 *  - A02 : l'endpoint interne (valeurs déchiffrées) exige le rôle SERVICE (@PreAuthorize).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ConfigSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus",
                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter c = new JwtGrantedAuthoritiesConverter();
        c.setAuthoritiesClaimName("roles");
        c.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter j = new JwtAuthenticationConverter();
        j.setJwtGrantedAuthoritiesConverter(c);
        return j;
    }
}

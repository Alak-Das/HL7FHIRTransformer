package com.al.hl7fhirtransformer.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyAuthFilter apiKeyAuthFilter;

    @Autowired
    public SecurityConfig(ApiKeyAuthFilter apiKeyAuthFilter) {
        this.apiKeyAuthFilter = apiKeyAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                // Register API Key filter BEFORE Basic Auth so X-API-Key headers are honoured
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Actuator — admin only
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // Tenant management — admin only
                        .requestMatchers("/api/tenants", "/api/tenants/**").hasRole("ADMIN")
                        // Conversion endpoints — admin or tenant
                        .requestMatchers("/api/convert/**").hasAnyRole("ADMIN", "TENANT")
                        // ACK retrieval — admin or tenant
                        .requestMatchers("/api/ack/**").hasAnyRole("ADMIN", "TENANT")
                        // Subscription management — admin or tenant
                        .requestMatchers("/api/subscriptions/**").hasAnyRole("ADMIN", "TENANT")
                        // Health GET — any authenticated user; cache DELETE — admin only
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/health")
                        .hasAnyRole("ADMIN", "TENANT")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/health/cache/**")
                        .hasRole("ADMIN")
                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .httpBasic(withDefaults());
        return http.build();
    }
}

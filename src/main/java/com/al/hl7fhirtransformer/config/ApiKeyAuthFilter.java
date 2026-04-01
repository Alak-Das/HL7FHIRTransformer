package com.al.hl7fhirtransformer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servlet filter that authenticates requests carrying an {@code X-API-Key}
 * header.
 *
 * <p>
 * API keys are configured in {@code application.properties} using the prefix
 * {@code app.api-keys.<principalName>=<key>}, for example:
 * 
 * <pre>
 *   app.api-keys.system1=${API_KEY_SYSTEM1:changeme}
 *   app.api-keys.system2=${API_KEY_SYSTEM2:changeme2}
 * </pre>
 *
 * <p>
 * On a successful key match:
 * </p>
 * <ul>
 * <li>The principal name is set to the configured key name (e.g.
 * {@code system1})</li>
 * <li>The role {@code ROLE_TENANT} is granted so the caller can reach
 * conversion endpoints</li>
 * </ul>
 *
 * <p>
 * This filter runs before Spring Security's
 * {@code UsernamePasswordAuthenticationFilter}, so if a valid API key is
 * provided, Basic Auth is not required. If the header is absent, the filter
 * passes the request through unchanged (Basic Auth still applies).
 * </p>
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY_PREFIX = "app.api-keys.";

    /** Loaded once at filter instantiation from application properties. */
    private final Map<String, String> keyToPrincipal = new ConcurrentHashMap<>();

    @Autowired
    public ApiKeyAuthFilter(Environment env) {
        // Dynamically discover all app.api-keys.* properties from all property sources.
        // Adding a new key only requires a new line in application.properties — no code change.
        if (env instanceof org.springframework.core.env.ConfigurableEnvironment configEnv) {
            for (org.springframework.core.env.PropertySource<?> ps : configEnv.getPropertySources()) {
                if (ps instanceof org.springframework.core.env.EnumerablePropertySource<?> eps) {
                    for (String propName : eps.getPropertyNames()) {
                        if (propName.startsWith(API_KEY_PREFIX)) {
                            String name = propName.substring(API_KEY_PREFIX.length());
                            String keyValue = env.getProperty(propName);
                            if (keyValue != null && !keyValue.isBlank()) {
                                keyToPrincipal.put(keyValue, name);
                                log.info("API Key registered for principal: {}", name);
                            }
                        }
                    }
                }
            }
        }

        if (keyToPrincipal.isEmpty()) {
            log.warn("No API keys configured under prefix '{}'", API_KEY_PREFIX);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null && !apiKey.isBlank()) {
            String principal = keyToPrincipal.get(apiKey);
            if (principal != null) {
                // Valid API key — set up authentication in SecurityContext
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_TENANT")));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("API Key authentication successful for principal: {}", principal);
            } else {
                log.warn("Invalid API Key received from {}", request.getRemoteAddr());
                // Don't short-circuit — let the security chain reject it properly
            }
        }

        filterChain.doFilter(request, response);
    }
}

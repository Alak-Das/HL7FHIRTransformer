package com.al.hl7fhirtransformer.config;

import com.al.hl7fhirtransformer.interceptor.RateLimitInterceptor;
import com.al.hl7fhirtransformer.interceptor.MdcInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final MdcInterceptor mdcInterceptor;

    public WebConfig(TenantInterceptor tenantInterceptor, RateLimitInterceptor rateLimitInterceptor,
            MdcInterceptor mdcInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.mdcInterceptor = mdcInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor);
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**") // Apply to all API endpoints
                .excludePathPatterns("/actuator/**"); // Exclude actuator endpoints

        // Register MDC Interceptor for all requests to ensure traceability
        registry.addInterceptor(mdcInterceptor);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }

    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

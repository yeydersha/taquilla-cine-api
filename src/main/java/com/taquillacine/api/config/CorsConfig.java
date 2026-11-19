package com.taquillacine.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Configuración CORS centralizada para la integración con Angular 21.
 *
 * <p>Solo se autorizan los orígenes indicados en la propiedad
 * {@code app.cors.allowed-origins} (variable de entorno
 * {@code APP_CORS_ALLOWED_ORIGINS}). El valor predeterminado es el servidor de
 * desarrollo de Angular: {@code http://localhost:4200}. No se usa el comodín
 * {@code *} ni se habilitan credenciales.</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origenesPermitidos;

    public CorsConfig(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String origenesPermitidos) {
        this.origenesPermitidos = origenesPermitidos != null
                ? Arrays.stream(origenesPermitidos.split(","))
                        .map(s -> s.trim())
                        .filter(origen -> !origen.isEmpty())
                        .toArray(String[]::new)
                : new String[0];
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "Origin", "X-Requested-With")
                .exposedHeaders("Location")
                .maxAge(3600);
    }
}

package com.fractal.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Authorize Requests
                .authorizeHttpRequests(auth -> auth
                        // Allow unauthenticated access to the health check endpoint
                        .requestMatchers("/api/health").permitAll()
                        // All other requests under /api/ must be authenticated
                        .requestMatchers("/api/**").authenticated()
                        // Any other request (like serving the frontend) can be permitted
                        .anyRequest().permitAll())
                // 2. Configure OAuth2 Login
                .oauth2Login(withDefaults()); // This enables the default Google login flow

        return http.build();
    }
}
package com.fractal.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fractal.backend.security.CustomOAuth2AuthenticationSuccessHandler;
import com.fractal.backend.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        @Order(1)
        public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/oauth2/**")
                                .cors(cors -> cors.disable())
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2.successHandler(customOAuth2AuthenticationSuccessHandler));
                return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**")
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())

                                // 🚫 NO REDIRECTS — API behavior
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        // --- DEBUG LOG START ---
                                                        System.err.println(
                                                                        ">>> [SECURITY ERROR] 401 Unauthorized Triggered in EntryPoint");
                                                        System.err.println(">>> [SECURITY ERROR] Exception: "
                                                                        + authException.getMessage());
                                                        System.err.println(">>> [SECURITY ERROR] Request URI: "
                                                                        + request.getRequestURI());
                                                        // --- DEBUG LOG END ---

                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                }))

                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/health").permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(List.of(
                                "http://localhost:3000",
                                "http://lvh.me:3000",
                                "http://*.lvh.me:3000",
                                "https://app.rishabhxchoudhary.com",
                                "https://*.app.rishabhxchoudhary.com"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", configuration);
                return source;
        }
}
package com.fractal.backend.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fractal.backend.model.User;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @lombok.NonNull HttpServletRequest request,
            @lombok.NonNull HttpServletResponse response,
            @lombok.NonNull FilterChain filterChain) throws ServletException, IOException {

        // --- DEBUG LOG 1: Entry Point ---
        System.out.println(">>> [JWT FILTER] Entered filter for URI: " + request.getRequestURI());

        final String authHeader = request.getHeader("Authorization");

        // --- DEBUG LOG 2: Check Header ---
        System.out.println(">>> [JWT FILTER] Authorization Header found: " + (authHeader != null));

        final String jwt;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // --- DEBUG LOG 3: No Token Logic ---
            System.out.println(">>> [JWT FILTER] No Bearer token found. Passing request down the chain.");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Remove "Bearer "
        try {
            userEmail = jwtService.extractUsername(jwt);

            // 2. If user is found and not already authenticated in this context
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 3. Load user from DB
                User user = userRepository.findByEmail(userEmail).orElse(null);

                if (user != null && jwtService.isTokenValid(jwt, user.getEmail())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            null);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println(">>> [JWT FILTER] User authenticated successfully via JWT: " + userEmail);
                }
            }
        } catch (Exception e) {
            // Token invalid or expired
            System.err.println(">>> [JWT FILTER] Exception processing token: " + e.getMessage());
        }

        // --- DEBUG LOG 4: Chain Continuation ---
        System.out.println(">>> [JWT FILTER] Proceeding with filter chain...");
        filterChain.doFilter(request, response);
    }
}
package com.fractal.backend.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fractal.backend.dto.LoginResponse;
import com.fractal.backend.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j // Lombok annotation for logging
public class CustomOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    // We will get this value from our application.properties or .env file
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 1. Cast the authentication object to get user details
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();

        // 2. Extract user attributes provided by Google
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String avatarUrl = oauthUser.getAttribute("picture");

        // 3. Use our existing AuthService to find or create the user
        log.info("User {} successfully authenticated. Processing login/signup.", email);
        LoginResponse loginResponse = authService.loginOrSignup(email, name, avatarUrl);

        // 4. Determine where to redirect the user
        String redirectUrl;
        if (loginResponse.getWorkspaces().isEmpty()) {
            // If the user has no workspaces, send them to a "create workspace" page
            redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/welcome/new-workspace")
                .toUriString();
            log.info("New user or user with no workspaces. Redirecting to create workspace page.");
        } else {
            // If the user has workspaces, send them to the dashboard or a selector page
            // For now, let's just redirect to a generic dashboard path
            redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/dashboard")
                .toUriString();
             log.info("Existing user with workspaces. Redirecting to dashboard.");
        }
        
        // TODO: Later we will add a JWT token to the redirect URL for session management

        // 5. Perform the redirect
        response.sendRedirect(redirectUrl);
    }
}
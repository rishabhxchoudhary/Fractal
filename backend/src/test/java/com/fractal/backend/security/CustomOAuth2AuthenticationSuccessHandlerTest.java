package com.fractal.backend.security;

import com.fractal.backend.dto.LoginResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private CustomOAuth2AuthenticationSuccessHandler successHandler;

    private OAuth2User oAuth2User;
    private final String frontendUrl = "http://localhost:3000";

    @BeforeEach
    void setUp() {
        // Manually set the frontendUrl field since @Value won't work in a plain Mockito test
        ReflectionTestUtils.setField(successHandler, "frontendUrl", frontendUrl);

        Map<String, Object> attributes = Map.of(
            "email", "test.user@google.com",
            "name", "Test User",
            "picture", "http://example.com/avatar.jpg"
        );
        oAuth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "email");
    }

    @Test
    void onAuthenticationSuccess_newUser_redirectsToNewWorkspace() throws IOException, ServletException {
        // --- Arrange ---
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(oAuth2User, Collections.emptyList(), "google");

        User user = new User();
        user.setEmail("test.user@google.com");
        LoginResponse loginResponse = LoginResponse.builder()
                .user(user)
                .workspaces(new ArrayList<>()) // No workspaces
                .build();
        
        when(authService.loginOrSignup(anyString(), anyString(), anyString())).thenReturn(loginResponse);

        // --- Act ---
        successHandler.onAuthenticationSuccess(request, response, token);

        // --- Assert ---
        verify(authService).loginOrSignup(
            "test.user@google.com", 
            "Test User", 
            "http://example.com/avatar.jpg"
        );

        // Verify that the redirect goes to the "new workspace" page
        verify(response).sendRedirect(frontendUrl + "/welcome/new-workspace"); 
    }

    @Test
    void onAuthenticationSuccess_existingUser_redirectsToDashboard() throws IOException, ServletException {
        // --- Arrange ---
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(oAuth2User, Collections.emptyList(), "google");

        User user = new User();
        user.setEmail("test.user@google.com");
        
        // Simulate a user that has an existing workspace
        List<LoginResponse.WorkspaceDTO> workspaces = List.of(
            LoginResponse.WorkspaceDTO.builder()
                .id(UUID.randomUUID())
                .name("My Workspace")
                .slug("my-workspace")
                .role("OWNER")
                .build()
        );
        LoginResponse loginResponse = LoginResponse.builder()
                .user(user)
                .workspaces(workspaces)
                .build();
        
        when(authService.loginOrSignup(anyString(), anyString(), anyString())).thenReturn(loginResponse);

        // --- Act ---
        successHandler.onAuthenticationSuccess(request, response, token);

        // --- Assert ---
        verify(authService).loginOrSignup(
            "test.user@google.com", 
            "Test User", 
            "http://example.com/avatar.jpg"
        );

        // Verify that the redirect goes to the dashboard
        verify(response).sendRedirect(frontendUrl + "/dashboard"); 
    }
}

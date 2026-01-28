package com.fractal.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fractal.backend.config.SecurityConfig;
import com.fractal.backend.controller.HealthCheckController;
import com.fractal.backend.security.CustomOAuth2AuthenticationSuccessHandler;

@WebMvcTest(HealthCheckController.class)
@Import(SecurityConfig.class)
public class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // We mock this bean so SecurityConfig can load without needing the real
    // AuthService
    @MockitoBean
    private CustomOAuth2AuthenticationSuccessHandler successHandler;

    @Test
    void shouldAllowAccessToPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessToProtectedEndpointWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isFound()); // 302 Redirect to Login
    }
}
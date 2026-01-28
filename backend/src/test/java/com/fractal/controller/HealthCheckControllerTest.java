package com.fractal.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fractal.backend.config.SecurityConfig;
import com.fractal.backend.controller.HealthCheckController;

@WebMvcTest(HealthCheckController.class) // Focus test on this controller
@Import(SecurityConfig.class) // Import our new security config
public class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowAccessToPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessToProtectedEndpointWithoutAuth() throws Exception {
        // When we access a protected endpoint without being logged in,
        // Spring Security should redirect us to the login page.
        // The HTTP status for a redirect is 302 (Found).
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isFound());
    }
}
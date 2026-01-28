package com.fractal.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fractal.backend.model.User;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public User getCurrentUser(@AuthenticationPrincipal User user) {
        // The 'user' is injected by our JwtAuthenticationFilter
        return user;
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        // Since we are stateless (JWT), logout is mostly client-side (clearing
        // localStorage).
        // But we can clear any cookies if we set them.
    }
}
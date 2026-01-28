package com.fractal.backend.service;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fractal.backend.dto.LoginResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    

    @Transactional
    public LoginResponse loginOrSignup(String email, String fullName, String avatarUrl) {
        // 1. Try to find user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 2. If not found, create new
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName(fullName);
                    newUser.setAvatarUrl(avatarUrl);
                    return userRepository.save(newUser);
                });

        // 3. TODO: Fetch workspaces for this user (Empty for now)
        
        return LoginResponse.builder()
                .user(user)
                .workspaces(new ArrayList<>()) 
                .build();
    }
}
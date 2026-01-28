package com.fractal.service;

import com.fractal.backend.dto.LoginResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito for this test class
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.fractal.backend.repository.WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_ShouldCreateNewUser_WhenUserDoesNotExist() {
        // Arrange
        String email = "new@example.com";
        String name = "New User";
        String avatar = "http://avatar.url";

        // Mock DB returning empty (User not found)
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        
        // Mock DB saving a user (return the user that was passed in)
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(java.util.UUID.randomUUID()); // Simulate DB generating ID
            return u;
        });

        // Act
        LoginResponse response = authService.loginOrSignup(email, name, avatar);

        // Assert
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo(email);
        assertThat(response.getUser().getFullName()).isEqualTo(name);
        assertThat(response.getWorkspaces()).isEmpty(); // New user has no workspaces

        // Verify save was called exactly once
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void login_ShouldReturnExistingUser_WhenUserExists() {
        // Arrange
        String email = "existing@example.com";
        User existingUser = new User();
        existingUser.setEmail(email);
        existingUser.setFullName("Old Name");

        // Mock DB finding the user
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

        // Act
        LoginResponse response = authService.loginOrSignup(email, "New Name", "avatar");

        // Assert
        assertThat(response.getUser()).isEqualTo(existingUser);
        verify(userRepository, never()).save(any(User.class)); // Should NOT save again
    }
}
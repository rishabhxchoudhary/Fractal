package com.fractal.controller;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper; // Import ObjectMapper
import com.fractal.backend.config.TestSecurityConfig;
import com.fractal.backend.controller.WorkspaceController;
import com.fractal.backend.dto.CreateWorkspaceRequest;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.security.CustomOAuth2AuthenticationSuccessHandler;
import com.fractal.backend.service.WorkspaceService;

@WebMvcTest(WorkspaceController.class)
@Import(TestSecurityConfig.class)
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Use `new ObjectMapper()` directly instead of Autowiring if config is tricky
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private WorkspaceService workspaceService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CustomOAuth2AuthenticationSuccessHandler successHandler;

    @Test
    void createWorkspace_ShouldReturnCreatedWorkspace() throws Exception {
        // Arrange
        String email = "test@fractal.com";
        UUID userId = UUID.randomUUID();
        String workspaceName = "My New Company";

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail(email);

        Workspace mockWorkspace = Workspace.builder()
                .id(UUID.randomUUID())
                .name(workspaceName)
                .slug("my-new-company")
                .ownerId(userId)
                .build();

        // Mock DB finding the user by email
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        // Mock Service creating the workspace
        when(workspaceService.createWorkspace(eq(userId), eq(workspaceName))).thenReturn(mockWorkspace);

        CreateWorkspaceRequest request = new CreateWorkspaceRequest();
        request.setName(workspaceName);

        // Act & Assert
        mockMvc.perform(post("/api/workspaces")
                .with(oauth2Login().attributes(attrs -> attrs.put("email", email)))
                .with(csrf()) 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))) // Use the manual instance
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(workspaceName))
                .andExpect(jsonPath("$.slug").value("my-new-company"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }
}
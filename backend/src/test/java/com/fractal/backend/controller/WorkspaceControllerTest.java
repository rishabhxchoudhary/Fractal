package com.fractal.backend.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fractal.backend.dto.CreateWorkspaceRequest;
import com.fractal.backend.dto.WorkspaceResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.security.JwtAuthenticationFilter;
import com.fractal.backend.service.WorkspaceService;

@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkspaceControllerTest {

        @Autowired
        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private WorkspaceService workspaceService;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Test
        void createWorkspace_ShouldReturnCreatedWorkspace() throws Exception {
                UUID userId = UUID.randomUUID();

                User user = new User();
                user.setId(userId);
                user.setEmail("test@fractal.com");

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

                Workspace workspace = Workspace.builder()
                                .id(UUID.randomUUID())
                                .name("My New Company")
                                .slug("my-new-company")
                                .ownerId(userId)
                                .build();

                when(workspaceService.createWorkspace(eq(userId), eq("My New Company")))
                                .thenReturn(workspace);

                CreateWorkspaceRequest request = new CreateWorkspaceRequest();
                request.setName("My New Company");

                mockMvc.perform(post("/api/workspaces")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("My New Company"));
        }

        @Test
        void getUserWorkspaces_ShouldReturnList() throws Exception {
                UUID userId = UUID.randomUUID();

                User user = new User();
                user.setId(userId);
                user.setEmail("test@fractal.com");

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

                WorkspaceResponse ws = WorkspaceResponse.builder()
                                .id(UUID.randomUUID())
                                .name("WS 1")
                                .slug("ws-1")
                                .role("OWNER")
                                .build();

                when(workspaceService.getWorkspacesForUser(userId))
                                .thenReturn(List.of(ws));

                mockMvc.perform(get("/api/workspaces"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("WS 1"));
        }
}

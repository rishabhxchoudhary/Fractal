package com.fractal.backend.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fractal.backend.dto.AddProjectMemberRequest;
import com.fractal.backend.dto.CreateProjectRequest;
import com.fractal.backend.dto.ProjectMemberDTO;
import com.fractal.backend.dto.ProjectResponse;
import com.fractal.backend.dto.TransferProjectOwnershipRequest;
import com.fractal.backend.dto.UpdateProjectMemberRequest;
import com.fractal.backend.model.Project;
import com.fractal.backend.model.User;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.security.JwtAuthenticationFilter;
import com.fractal.backend.service.JwtService;
import com.fractal.backend.service.ProjectService;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ProjectService projectService;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private UserRepository userRepository;

        private UUID userId;
        private UUID workspaceId;
        private UUID projectId;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                workspaceId = UUID.randomUUID();
                projectId = UUID.randomUUID();

                // 1. Manually set the Authentication Principal to our custom User entity
                // This is crucial because @WithMockUser creates a Spring UserDetails, not your
                // entity
                User mockUser = User.builder().id(userId).email("test@fractal.com").build();
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(mockUser, null,
                                Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
        }

        private User setupMockUser(UUID userId) {
                User user = new User();
                user.setId(userId);
                user.setEmail("test@fractal.com");

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
                return user;
        }

        // ==================================================================================
        // 1. CREATE PROJECT POST /api/workspaces/{workspaceId}/projects
        // ==================================================================================

        @Test
        @DisplayName("Create Project - Success")
        void createProject_Success() throws Exception {
                CreateProjectRequest request = new CreateProjectRequest();
                request.setName("New Project");
                request.setColor("#000000");

                Project mockProject = Project.builder().id(projectId).name("New Project").build();

                when(projectService.createProject(eq(userId), eq(workspaceId), eq("New Project"), eq("#000000"), any()))
                                .thenReturn(mockProject);

                mockMvc.perform(post("/api/workspaces/{workspaceId}/projects", workspaceId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(projectId.toString()))
                                .andExpect(jsonPath("$.role").value("OWNER"));
        }

        @Test
        @DisplayName("Create Project - 400 Bad Request (Validation Name Blank)")
        void createProject_ValidationFail() throws Exception {
                CreateProjectRequest request = new CreateProjectRequest();
                request.setName(""); // Invalid @NotBlank

                mockMvc.perform(post("/api/workspaces/{workspaceId}/projects", workspaceId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest()); // Verifies @Valid works
        }

        // ==================================================================================
        // 2. GET PROJECTS GET /api/workspaces/{workspaceId}/projects
        // ==================================================================================

        @Test
        @DisplayName("Get Projects - Success")
        void getProjects_Success() throws Exception {
                ProjectResponse response = ProjectResponse.builder().id(projectId).name("Demo").build();
                when(projectService.getProjects(userId, workspaceId)).thenReturn(List.of(response));

                mockMvc.perform(get("/api/workspaces/{workspaceId}/projects", workspaceId)
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1));
        }

        // ==================================================================================
        // 3. UPDATE PROJECT PUT /api/projects/{projectId}
        // ==================================================================================

        @Test
        @DisplayName("Update Project - Success")
        void updateProject_Success() throws Exception {
                CreateProjectRequest request = new CreateProjectRequest();
                request.setName("Updated Name");
                request.setColor("#FFFFFF");

                Project mockProject = Project.builder().id(projectId).name("Updated Name").build();
                when(projectService.updateProject(userId, projectId, "Updated Name", "#FFFFFF"))
                                .thenReturn(mockProject);

                mockMvc.perform(put("/api/projects/{projectId}", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Updated Name"));
        }

        @Test
        @DisplayName("Update Project - 403 Forbidden (Not Admin)")
        void updateProject_Forbidden() throws Exception {
                CreateProjectRequest request = new CreateProjectRequest();
                request.setName("Hacked");

                when(projectService.updateProject(any(), any(), any(), any()))
                                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                "Insufficient permissions"));

                mockMvc.perform(put("/api/projects/{projectId}", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        // ==================================================================================
        // 4. DELETE PROJECT DELETE /api/projects/{projectId}
        // ==================================================================================

        @Test
        @DisplayName("Delete Project - Success")
        void deleteProject_Success() throws Exception {
                mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(projectService).deleteProject(userId, projectId);
        }

        @Test
        @DisplayName("Delete Project - 404 Not Found")
        void deleteProject_NotFound() throws Exception {
                doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                                .when(projectService).deleteProject(userId, projectId);

                mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                                .with(csrf()))
                                .andExpect(status().isNotFound());
        }

        // ==================================================================================
        // 5. GET MEMBERS GET /api/projects/{projectId}/members
        // ==================================================================================

        @Test
        @DisplayName("Get Members - Success")
        void getMembers_Success() throws Exception {
                ProjectMemberDTO member = new ProjectMemberDTO(userId, "email", "name", "url", "OWNER", null);
                when(projectService.getProjectMembers(userId, projectId)).thenReturn(List.of(member));

                mockMvc.perform(get("/api/projects/{projectId}/members", projectId)
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].role").value("OWNER"));
        }

        // ==================================================================================
        // 6. ADD MEMBER POST /api/projects/{projectId}/members
        // ==================================================================================

        @Test
        @DisplayName("Add Member - Success")
        void addMember_Success() throws Exception {
                AddProjectMemberRequest request = new AddProjectMemberRequest();
                request.setUserId(UUID.randomUUID());
                request.setRole("EDITOR");

                mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(projectService).addMember(eq(userId), eq(projectId), eq(request.getUserId()), eq("EDITOR"));
        }

        @Test
        @DisplayName("Add Member - 400 Bad Request (Missing UserID)")
        void addMember_ValidationFail() throws Exception {
                AddProjectMemberRequest request = new AddProjectMemberRequest();
                request.setRole("EDITOR");
                // UserId is null

                mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Add Member - 409 Conflict (Already Member)")
        void addMember_Conflict() throws Exception {
                UUID userId = UUID.randomUUID();
                AddProjectMemberRequest request = new AddProjectMemberRequest();
                request.setUserId(userId);
                request.setRole("EDITOR"); // Explicitly set the role
                setupMockUser(userId);

                doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member"))
                                .when(projectService).addMember(
                                                eq(userId), // The user ID from the security context
                                                eq(projectId), // The project ID from the path
                                                eq(request.getUserId()), // The user ID from the request body
                                                eq(request.getRole()) // The role from the request body
                                );

                mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                                // csrf() is not needed when addFilters=false
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict()); // Now correctly expects and gets 409
        }

        // ==================================================================================
        // 7. UPDATE MEMBER ROLE PUT /api/projects/{projectId}/members/{userId}
        // ==================================================================================

        @Test
        @DisplayName("Update Member Role - Success")
        void updateMemberRole_Success() throws Exception {
                UpdateProjectMemberRequest request = new UpdateProjectMemberRequest();
                request.setRole("ADMIN");
                UUID targetUser = UUID.randomUUID();

                mockMvc.perform(put("/api/projects/{projectId}/members/{targetUser}", projectId, targetUser)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(projectService).updateMemberRole(userId, projectId, targetUser, "ADMIN");
        }

        @Test
        @DisplayName("Update Member Role - 400 Bad Request (Blank Role)")
        void updateMemberRole_ValidationFail() throws Exception {
                UpdateProjectMemberRequest request = new UpdateProjectMemberRequest();
                request.setRole(""); // Blank

                mockMvc.perform(put("/api/projects/{projectId}/members/{userId}", projectId, UUID.randomUUID())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // ==================================================================================
        // 8. REMOVE MEMBER DELETE /api/projects/{projectId}/members/{userId}
        // ==================================================================================

        @Test
        @DisplayName("Remove Member - Success")
        void removeMember_Success() throws Exception {
                UUID targetUser = UUID.randomUUID();

                mockMvc.perform(delete("/api/projects/{projectId}/members/{targetUser}", projectId, targetUser)
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(projectService).removeMember(userId, projectId, targetUser);
        }

        @Test
        @DisplayName("Remove Member - 403 Forbidden (Trying to remove Owner)")
        void removeMember_Forbidden() throws Exception {
                UUID targetUser = UUID.randomUUID();
                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove owner"))
                                .when(projectService).removeMember(userId, projectId, targetUser);

                mockMvc.perform(delete("/api/projects/{projectId}/members/{targetUser}", projectId, targetUser)
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        // ==================================================================================
        // 9. TRANSFER OWNERSHIP POST /api/projects/{projectId}/transfer-ownership
        // ==================================================================================

        @Test
        @DisplayName("Transfer Ownership - Success")
        void transferOwnership_Success() throws Exception {
                TransferProjectOwnershipRequest request = new TransferProjectOwnershipRequest();
                request.setNewOwnerId(UUID.randomUUID());

                mockMvc.perform(post("/api/projects/{projectId}/transfer-ownership", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Transfer Ownership - 400 Bad Request (Null Owner ID)")
        void transferOwnership_ValidationFail() throws Exception {
                TransferProjectOwnershipRequest request = new TransferProjectOwnershipRequest();
                // newOwnerId is null

                mockMvc.perform(post("/api/projects/{projectId}/transfer-ownership", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }
}
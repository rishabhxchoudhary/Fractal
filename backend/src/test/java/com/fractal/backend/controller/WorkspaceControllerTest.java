package com.fractal.backend.controller;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fractal.backend.dto.CreateWorkspaceRequest;
import com.fractal.backend.dto.InviteMemberRequest;
import com.fractal.backend.dto.TransferOwnershipRequest;
import com.fractal.backend.dto.UpdateMemberRoleRequest;
import com.fractal.backend.dto.UpdateWorkspaceRequest;
import com.fractal.backend.dto.WorkspaceMemberDTO;
import com.fractal.backend.dto.WorkspaceResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.model.WorkspaceMember;
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

        // --- HELPER FOR AUTH MOCKING ---
        private User setupMockUser(UUID userId) {
                User user = new User();
                user.setId(userId);
                user.setEmail("test@fractal.com");

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
                return user;
        }

        @Test
        void createWorkspace_ShouldReturnCreatedWorkspace() throws Exception {
                UUID userId = UUID.randomUUID();
                setupMockUser(userId);

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
                setupMockUser(userId);

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

        @Test
        void updateWorkspace_ShouldReturnUpdatedWorkspace() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(userId);

                Workspace updatedWorkspace = Workspace.builder()
                                .id(workspaceId)
                                .name("Updated Company")
                                .slug("updated-company")
                                .ownerId(userId)
                                .build();

                when(workspaceService.updateWorkspace(eq(userId), eq(workspaceId), eq("Updated Company"),
                                eq("updated-company")))
                                .thenReturn(updatedWorkspace);

                UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
                request.setName("Updated Company");
                request.setSlug("updated-company");

                mockMvc.perform(put("/api/workspaces/" + workspaceId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Updated Company"))
                                .andExpect(jsonPath("$.slug").value("updated-company"))
                                .andExpect(jsonPath("$.role").value("UNKNOWN"));
        }

        @Test
        void deleteWorkspace_ShouldReturnNoContent() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(userId);

                doNothing().when(workspaceService).deleteWorkspace(eq(userId), eq(workspaceId));

                mockMvc.perform(delete("/api/workspaces/" + workspaceId)
                                .with(csrf()))
                                .andExpect(status().isNoContent());
        }

        // --- MEMBER MANAGEMENT TESTS ---

        @Test
        void getWorkspaceMembers_ShouldReturnList() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(userId);

                WorkspaceMemberDTO memberDTO = WorkspaceMemberDTO.builder()
                                .id(UUID.randomUUID())
                                .email("member@fractal.com")
                                .fullName("John Doe")
                                .role("MEMBER")
                                .joinedAt(OffsetDateTime.now())
                                .build();

                when(workspaceService.getWorkspaceMembers(eq(userId), eq(workspaceId)))
                                .thenReturn(List.of(memberDTO));

                mockMvc.perform(get("/api/workspaces/" + workspaceId + "/members"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].email").value("member@fractal.com"))
                                .andExpect(jsonPath("$[0].role").value("MEMBER"));
        }

        @Test
        void removeMember_ShouldReturnNoContent() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID targetMemberId = UUID.randomUUID();
                setupMockUser(userId);

                doNothing().when(workspaceService).removeMember(eq(userId), eq(workspaceId), eq(targetMemberId));

                mockMvc.perform(delete("/api/workspaces/" + workspaceId + "/members/" + targetMemberId)
                                .with(csrf()))
                                .andExpect(status().isNoContent());
        }

        @Test
        void updateMemberRole_ShouldReturnOk() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID targetMemberId = UUID.randomUUID();
                setupMockUser(userId);

                doNothing().when(workspaceService).updateMemberRole(eq(userId), eq(workspaceId), eq(targetMemberId),
                                eq("ADMIN"));

                UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
                request.setRole("ADMIN");

                mockMvc.perform(put("/api/workspaces/" + workspaceId + "/members/" + targetMemberId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void transferOwnership_ShouldReturnOk() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID newOwnerId = UUID.randomUUID();
                setupMockUser(userId);

                doNothing().when(workspaceService).transferOwnership(eq(userId), eq(workspaceId), eq(newOwnerId));

                TransferOwnershipRequest request = new TransferOwnershipRequest();
                request.setNewOwnerId(newOwnerId);

                mockMvc.perform(post("/api/workspaces/" + workspaceId + "/transfer-ownership")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        // --- INVITATION TESTS ---

        @Test
        void inviteMember_ShouldReturnOk() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(userId);

                doNothing().when(workspaceService).inviteMember(eq(userId), eq(workspaceId), eq("new@example.com"),
                                eq("MEMBER"));

                InviteMemberRequest request = new InviteMemberRequest();
                request.setEmail("new@example.com");
                request.setRole("MEMBER");

                mockMvc.perform(post("/api/workspaces/" + workspaceId + "/invite")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void acceptInvite_ShouldReturnOk() throws Exception {
                UUID userId = UUID.randomUUID();
                setupMockUser(userId);

                WorkspaceMember member = WorkspaceMember.builder().build();

                when(workspaceService.acceptInvitation(eq(userId), eq("test-token")))
                                .thenReturn(member);

                mockMvc.perform(post("/api/workspaces/accept-invite")
                                .with(csrf())
                                .param("token", "test-token"))
                                .andExpect(status().isOk());
        }
}
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

        // --- RBAC PERMISSION TESTS ---

        @Test
        void updateWorkspace_AsOwner_ShouldSucceed() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(ownerId);

                Workspace updatedWorkspace = Workspace.builder()
                                .id(workspaceId)
                                .name("Updated Name")
                                .slug("updated-slug")
                                .ownerId(ownerId)
                                .build();

                when(workspaceService.updateWorkspace(eq(ownerId), eq(workspaceId), eq("Updated Name"),
                                eq("updated-slug")))
                                .thenReturn(updatedWorkspace);

                UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
                request.setName("Updated Name");
                request.setSlug("updated-slug");

                mockMvc.perform(put("/api/workspaces/" + workspaceId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void updateWorkspace_AsAdmin_ShouldSucceed() throws Exception {
                UUID adminId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(adminId);

                Workspace updatedWorkspace = Workspace.builder()
                                .id(workspaceId)
                                .name("Updated Name")
                                .slug("updated-slug")
                                .build();

                when(workspaceService.updateWorkspace(eq(adminId), eq(workspaceId), eq("Updated Name"),
                                eq("updated-slug")))
                                .thenReturn(updatedWorkspace);

                UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
                request.setName("Updated Name");
                request.setSlug("updated-slug");

                mockMvc.perform(put("/api/workspaces/" + workspaceId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void updateWorkspace_AsMember_ShouldReturnForbidden() throws Exception {
                UUID memberId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(memberId);

                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Insufficient permissions"))
                                .when(workspaceService).updateWorkspace(eq(memberId), eq(workspaceId), any(), any());

                UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
                request.setName("Updated Name");
                request.setSlug("updated-slug");

                mockMvc.perform(put("/api/workspaces/" + workspaceId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void deleteWorkspace_AsOwner_ShouldSucceed() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(ownerId);

                doNothing().when(workspaceService).deleteWorkspace(eq(ownerId), eq(workspaceId));

                mockMvc.perform(delete("/api/workspaces/" + workspaceId)
                                .with(csrf()))
                                .andExpect(status().isNoContent());
        }

        @Test
        void deleteWorkspace_AsAdmin_ShouldReturnForbidden() throws Exception {
                UUID adminId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(adminId);

                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Insufficient permissions"))
                                .when(workspaceService).deleteWorkspace(eq(adminId), eq(workspaceId));

                mockMvc.perform(delete("/api/workspaces/" + workspaceId)
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        @Test
        void deleteWorkspace_AsMember_ShouldReturnForbidden() throws Exception {
                UUID memberId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(memberId);

                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Insufficient permissions"))
                                .when(workspaceService).deleteWorkspace(eq(memberId), eq(workspaceId));

                mockMvc.perform(delete("/api/workspaces/" + workspaceId)
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        @Test
        void updateMemberRole_AsOwner_ShouldSucceed() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID targetMemberId = UUID.randomUUID();
                setupMockUser(ownerId);

                doNothing().when(workspaceService).updateMemberRole(eq(ownerId), eq(workspaceId), eq(targetMemberId),
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
        void updateMemberRole_AsAdmin_ShouldReturnForbidden() throws Exception {
                UUID adminId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID targetMemberId = UUID.randomUUID();
                setupMockUser(adminId);

                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Insufficient permissions"))
                                .when(workspaceService).updateMemberRole(eq(adminId), eq(workspaceId),
                                                eq(targetMemberId), any());

                UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
                request.setRole("ADMIN");

                mockMvc.perform(put("/api/workspaces/" + workspaceId + "/members/" + targetMemberId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void updateMemberRole_AsMember_ShouldReturnForbidden() throws Exception {
                UUID memberId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID targetMemberId = UUID.randomUUID();
                setupMockUser(memberId);

                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Insufficient permissions"))
                                .when(workspaceService).updateMemberRole(eq(memberId), eq(workspaceId),
                                                eq(targetMemberId), any());

                UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
                request.setRole("ADMIN");

                mockMvc.perform(put("/api/workspaces/" + workspaceId + "/members/" + targetMemberId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void updateMemberRole_WithInvalidRole_ShouldReturnBadRequest() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID targetMemberId = UUID.randomUUID();
                setupMockUser(ownerId);

                doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Invalid role. Allowed roles are: OWNER, ADMIN, MEMBER"))
                                .when(workspaceService).updateMemberRole(eq(ownerId), eq(workspaceId),
                                                eq(targetMemberId), eq("VIEWER"));

                UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
                request.setRole("VIEWER");

                mockMvc.perform(put("/api/workspaces/" + workspaceId + "/members/" + targetMemberId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void updateMemberRole_ToOwner_ShouldReturnForbidden() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID targetMemberId = UUID.randomUUID();
                setupMockUser(ownerId);

                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Ownership transfer must be done via specific endpoint"))
                                .when(workspaceService).updateMemberRole(eq(ownerId), eq(workspaceId),
                                                eq(targetMemberId), eq("OWNER"));

                UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
                request.setRole("OWNER");

                mockMvc.perform(put("/api/workspaces/" + workspaceId + "/members/" + targetMemberId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void removeMember_AsOwner_ShouldSucceed() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID targetMemberId = UUID.randomUUID();
                setupMockUser(ownerId);

                doNothing().when(workspaceService).removeMember(eq(ownerId), eq(workspaceId), eq(targetMemberId));

                mockMvc.perform(delete("/api/workspaces/" + workspaceId + "/members/" + targetMemberId)
                                .with(csrf()))
                                .andExpect(status().isNoContent());
        }

        @Test
        void removeMember_AsAdmin_ShouldReturnForbidden() throws Exception {
                UUID adminId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID targetMemberId = UUID.randomUUID();
                setupMockUser(adminId);

                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Only workspace owners can remove members"))
                                .when(workspaceService).removeMember(eq(adminId), eq(workspaceId), eq(targetMemberId));

                mockMvc.perform(delete("/api/workspaces/" + workspaceId + "/members/" + targetMemberId)
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        @Test
        void removeMember_AsMember_ShouldReturnForbidden() throws Exception {
                UUID memberId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID targetMemberId = UUID.randomUUID();
                setupMockUser(memberId);

                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Only workspace owners can remove members"))
                                .when(workspaceService).removeMember(eq(memberId), eq(workspaceId), eq(targetMemberId));

                mockMvc.perform(delete("/api/workspaces/" + workspaceId + "/members/" + targetMemberId)
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        @Test
        void inviteMember_AsOwner_WithMemberRole_ShouldSucceed() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(ownerId);

                doNothing().when(workspaceService).inviteMember(eq(ownerId), eq(workspaceId), eq("new@example.com"),
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
        void inviteMember_AsOwner_WithAdminRole_ShouldSucceed() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(ownerId);

                doNothing().when(workspaceService).inviteMember(eq(ownerId), eq(workspaceId), eq("admin@example.com"),
                                eq("ADMIN"));

                InviteMemberRequest request = new InviteMemberRequest();
                request.setEmail("admin@example.com");
                request.setRole("ADMIN");

                mockMvc.perform(post("/api/workspaces/" + workspaceId + "/invite")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void inviteMember_AsAdmin_WithMemberRole_ShouldSucceed() throws Exception {
                UUID adminId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(adminId);

                doNothing().when(workspaceService).inviteMember(eq(adminId), eq(workspaceId), eq("new@example.com"),
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
        void inviteMember_AsAdmin_WithAdminRole_ShouldReturnForbidden() throws Exception {
                UUID adminId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(adminId);

                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Only workspace owners can invite admins"))
                                .when(workspaceService).inviteMember(eq(adminId), eq(workspaceId),
                                                eq("admin@example.com"), eq("ADMIN"));

                InviteMemberRequest request = new InviteMemberRequest();
                request.setEmail("admin@example.com");
                request.setRole("ADMIN");

                mockMvc.perform(post("/api/workspaces/" + workspaceId + "/invite")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void inviteMember_AsMember_ShouldReturnForbidden() throws Exception {
                UUID memberId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(memberId);

                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Insufficient permissions"))
                                .when(workspaceService).inviteMember(eq(memberId), eq(workspaceId), any(), any());

                InviteMemberRequest request = new InviteMemberRequest();
                request.setEmail("new@example.com");
                request.setRole("MEMBER");

                mockMvc.perform(post("/api/workspaces/" + workspaceId + "/invite")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void inviteMember_WithInvalidRole_ShouldReturnBadRequest() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(ownerId);

                doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Invalid role. Allowed roles are: OWNER, ADMIN, MEMBER"))
                                .when(workspaceService).inviteMember(eq(ownerId), eq(workspaceId),
                                                eq("new@example.com"), eq("VIEWER"));

                InviteMemberRequest request = new InviteMemberRequest();
                request.setEmail("new@example.com");
                request.setRole("VIEWER");

                mockMvc.perform(post("/api/workspaces/" + workspaceId + "/invite")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getWorkspaceMembers_AsOwner_ShouldSucceed() throws Exception {
                UUID ownerId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(ownerId);

                WorkspaceMemberDTO memberDTO = WorkspaceMemberDTO.builder()
                                .id(UUID.randomUUID())
                                .email("member@fractal.com")
                                .fullName("John Doe")
                                .role("MEMBER")
                                .joinedAt(OffsetDateTime.now())
                                .build();

                when(workspaceService.getWorkspaceMembers(eq(ownerId), eq(workspaceId)))
                                .thenReturn(List.of(memberDTO));

                mockMvc.perform(get("/api/workspaces/" + workspaceId + "/members"))
                                .andExpect(status().isOk());
        }

        @Test
        void getWorkspaceMembers_AsAdmin_ShouldSucceed() throws Exception {
                UUID adminId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(adminId);

                WorkspaceMemberDTO memberDTO = WorkspaceMemberDTO.builder()
                                .id(UUID.randomUUID())
                                .email("member@fractal.com")
                                .fullName("John Doe")
                                .role("MEMBER")
                                .joinedAt(OffsetDateTime.now())
                                .build();

                when(workspaceService.getWorkspaceMembers(eq(adminId), eq(workspaceId)))
                                .thenReturn(List.of(memberDTO));

                mockMvc.perform(get("/api/workspaces/" + workspaceId + "/members"))
                                .andExpect(status().isOk());
        }

        @Test
        void getWorkspaceMembers_AsMember_ShouldSucceed() throws Exception {
                UUID memberId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                setupMockUser(memberId);

                WorkspaceMemberDTO memberDTO = WorkspaceMemberDTO.builder()
                                .id(UUID.randomUUID())
                                .email("member@fractal.com")
                                .fullName("John Doe")
                                .role("MEMBER")
                                .joinedAt(OffsetDateTime.now())
                                .build();

                when(workspaceService.getWorkspaceMembers(eq(memberId), eq(workspaceId)))
                                .thenReturn(List.of(memberDTO));

                mockMvc.perform(get("/api/workspaces/" + workspaceId + "/members"))
                                .andExpect(status().isOk());
        }
}
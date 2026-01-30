package com.fractal.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fractal.backend.dto.CreateWorkspaceRequest;
import com.fractal.backend.dto.InviteMemberRequest;
import com.fractal.backend.dto.TransferOwnershipRequest;
import com.fractal.backend.dto.UpdateMemberRoleRequest;
import com.fractal.backend.dto.UpdateWorkspaceRequest;
import com.fractal.backend.dto.WorkspaceMemberDTO;
import com.fractal.backend.dto.WorkspaceResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.service.WorkspaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    // --- HELPER FOR AUTH CHECK ---
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    // --- WORKSPACE CRUD ---

    @PostMapping
    public WorkspaceResponse createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        User user = getAuthenticatedUser(); // Using helper to reduce code duplication, but same logic
        Workspace workspace = workspaceService.createWorkspace(user.getId(), request.getName());

        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .slug(workspace.getSlug())
                .role("OWNER")
                .build();
    }

    @GetMapping
    public List<WorkspaceResponse> getUserWorkspaces() {
        User user = getAuthenticatedUser();
        return workspaceService.getWorkspacesForUser(user.getId());
    }

    @PutMapping("/{id}")
    public WorkspaceResponse updateWorkspace(
            @PathVariable UUID id,
            @RequestBody UpdateWorkspaceRequest request) {
        User user = getAuthenticatedUser();
        Workspace updated = workspaceService.updateWorkspace(user.getId(), id, request.getName(), request.getSlug());
        return WorkspaceResponse.builder()
                .id(updated.getId())
                .name(updated.getName())
                .slug(updated.getSlug())
                .role("UNKNOWN")
                .build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkspace(@PathVariable UUID id) {
        User user = getAuthenticatedUser();
        workspaceService.deleteWorkspace(user.getId(), id);
    }

    // --- MEMBER MANAGEMENT ---

    @GetMapping("/{id}/members")
    public List<WorkspaceMemberDTO> getWorkspaceMembers(@PathVariable UUID id) {
        User user = getAuthenticatedUser();
        return workspaceService.getWorkspaceMembers(user.getId(), id);
    }

    @DeleteMapping("/{id}/members/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID id,
            @PathVariable UUID targetUserId) {
        User user = getAuthenticatedUser();
        workspaceService.removeMember(user.getId(), id, targetUserId);
    }

    @PutMapping("/{id}/members/{targetUserId}")
    public void updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        User user = getAuthenticatedUser();
        workspaceService.updateMemberRole(user.getId(), id, targetUserId, request.getRole());
    }

    @PostMapping("/{id}/transfer-ownership")
    public void transferOwnership(
            @PathVariable UUID id,
            @Valid @RequestBody TransferOwnershipRequest request) {
        User user = getAuthenticatedUser();
        workspaceService.transferOwnership(user.getId(), id, request.getNewOwnerId());
    }

    // --- INVITATIONS ---

    @PostMapping("/{id}/invite")
    public void inviteMember(
            @PathVariable UUID id,
            @Valid @RequestBody InviteMemberRequest request) {
        User user = getAuthenticatedUser();
        workspaceService.inviteMember(user.getId(), id, request.getEmail(), request.getRole());
    }

    @PostMapping("/accept-invite")
    public void acceptInvite(@RequestParam String token) {
        User user = getAuthenticatedUser();
        workspaceService.acceptInvitation(user.getId(), token);
    }
}
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
import com.fractal.backend.dto.UpdateWorkspaceRequest;
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

    @PostMapping
    public WorkspaceResponse createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        return workspaceService.getWorkspacesForUser(user.getId());
    }

    @PutMapping("/{id}")
    public WorkspaceResponse updateWorkspace(
            @PathVariable UUID id,
            @RequestBody UpdateWorkspaceRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        Workspace updated = workspaceService.updateWorkspace(user.getId(), id, request.getName(), request.getSlug());
        return WorkspaceResponse.builder().id(updated.getId()).name(updated.getName()).slug(updated.getSlug())
                .role("UNKNOWN").build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkspace(@PathVariable UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        workspaceService.deleteWorkspace(user.getId(), id);
    }

    @PostMapping("/{id}/invite")
    public void inviteMember(
            @PathVariable UUID id,
            @Valid @RequestBody InviteMemberRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        workspaceService.inviteMember(user.getId(), id, request.getEmail(), request.getRole());
    }

    @PostMapping("/accept-invite")
    public void acceptInvite(
            @RequestParam String token) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        workspaceService.acceptInvitation(user.getId(), token);
    }
}

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fractal.backend.dto.AddProjectMemberRequest;
import com.fractal.backend.dto.CreateProjectRequest;
import com.fractal.backend.dto.ProjectMemberDTO;
import com.fractal.backend.dto.ProjectResponse;
import com.fractal.backend.dto.TransferProjectOwnershipRequest;
import com.fractal.backend.dto.UpdateProjectMemberRequest;
import com.fractal.backend.model.Project;
import com.fractal.backend.model.User;
import com.fractal.backend.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    // --- PROJECT CRUD ---

    @PostMapping("/workspaces/{workspaceId}/projects")
    public ProjectResponse createProject(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateProjectRequest request) {

        User user = getAuthenticatedUser();
        Project p = projectService.createProject(
                user.getId(), workspaceId, request.getName(), request.getColor(), request.getParentId());

        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .role("OWNER")
                .parentId(p.getParentId())
                .build();
    }

    @GetMapping("/workspaces/{workspaceId}/projects")
    public List<ProjectResponse> getProjects(
            @PathVariable UUID workspaceId) {
        User user = getAuthenticatedUser();
        return projectService.getProjects(user.getId(), workspaceId);
    }

    @PutMapping("/projects/{projectId}")
    public ProjectResponse updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateProjectRequest request) {
        User user = getAuthenticatedUser();
        Project p = projectService.updateProject(
                user.getId(), projectId, request.getName(), request.getColor());
        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .build();
    }

    @DeleteMapping("/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(
            @PathVariable UUID projectId) {
        User user = getAuthenticatedUser();
        projectService.deleteProject(user.getId(), projectId);
    }

    // --- MEMBERSHIP ---

    @GetMapping("/projects/{projectId}/members")
    public List<ProjectMemberDTO> getMembers(
            @PathVariable UUID projectId) {
        User user = getAuthenticatedUser();
        return projectService.getProjectMembers(user.getId(), projectId);
    }

    @PostMapping("/projects/{projectId}/members")
    public void addMember(
            @PathVariable UUID projectId,
            @Valid @RequestBody AddProjectMemberRequest request) {
        User user = getAuthenticatedUser();
        projectService.addMember(user.getId(), projectId, request.getUserId(), request.getRole());
    }

    @PutMapping("/projects/{projectId}/members/{userId}")
    public void updateMemberRole(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProjectMemberRequest request) {
        User user = getAuthenticatedUser();
        projectService.updateMemberRole(user.getId(), projectId, userId, request.getRole());
    }

    @DeleteMapping("/projects/{projectId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID projectId,
            @PathVariable UUID userId) {
        User user = getAuthenticatedUser();
        projectService.removeMember(user.getId(), projectId, userId);
    }

    @PostMapping("/projects/{projectId}/transfer-ownership")
    public void transferOwnership(
            @PathVariable UUID projectId,
            @Valid @RequestBody TransferProjectOwnershipRequest request) {
        User user = getAuthenticatedUser();
        projectService.transferOwnership(user.getId(), projectId, request.getNewOwnerId());
    }
}
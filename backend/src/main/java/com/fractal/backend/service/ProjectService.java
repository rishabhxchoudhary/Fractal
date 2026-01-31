package com.fractal.backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fractal.backend.dto.ProjectMemberDTO;
import com.fractal.backend.dto.ProjectResponse;
import com.fractal.backend.model.Project;
import com.fractal.backend.model.ProjectMember;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.ProjectMemberRepository;
import com.fractal.backend.repository.ProjectRepository;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    // --- CRUD OPERATIONS ---

    @Transactional
    public Project createProject(UUID userId, UUID workspaceId, String name, String color, UUID parentId) {
        // 1. Verify Workspace Access
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this workspace"));

        // 2. Validate Parent (if exists)
        if (parentId != null) {
            boolean parentExists = projectRepository.existsByIdAndWorkspaceId(parentId, workspaceId);
            if (!parentExists) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent project not found in this workspace");
            }
            // User must have access to parent to create child
            projectMemberRepository.findByProjectIdAndUserId(parentId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "You don't have access to the parent project"));
        }

        // 3. Save Project
        Project project = Project.builder()
                .workspaceId(workspaceId)
                .parentId(parentId)
                .name(name)
                .color(color)
                .createdBy(userId)
                .build();
        Project savedProject = projectRepository.save(project);

        // 4. Build Hierarchy (Closure Table)
        projectRepository.insertSelfReference(savedProject.getId());
        if (parentId != null) {
            projectRepository.insertHierarchy(parentId, savedProject.getId());
        }

        // 5. Add Creator as OWNER
        ProjectMember owner = ProjectMember.builder()
                .projectId(savedProject.getId())
                .userId(userId)
                .role("OWNER")
                .build();
        projectMemberRepository.save(owner);

        // 6. Inherit Members from Parent (Snapshot Inheritance)
        if (parentId != null) {
            inheritMembers(parentId, savedProject.getId(), userId);
        }

        return savedProject;
    }

    public List<ProjectResponse> getProjects(UUID userId, UUID workspaceId) {
        // Ensure workspace access
        if (workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<Project> projects = projectRepository.findAllByWorkspaceIdAndUserId(workspaceId, userId);
        return projects.stream().map(p -> {
            ProjectMember pm = projectMemberRepository.findByProjectIdAndUserId(p.getId(), userId).orElse(null);
            return toResponse(p, pm != null ? pm.getRole() : "UNKNOWN");
        }).collect(Collectors.toList());
    }

    @Transactional
    public Project updateProject(UUID userId, UUID projectId, String name, String color) {
        validateProjectAdminAccess(userId, projectId); // Strict Permission Check

        Project project = getProjectOrThrow(projectId);
        if (name != null && !name.isBlank())
            project.setName(name);
        if (color != null)
            project.setColor(color);

        return projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(UUID userId, UUID projectId) {
        // Permission: Project OWNER or Workspace OWNER/ADMIN
        checkStrictPermission(userId, projectId, List.of("OWNER"));

        // 1. Get all descendants (Self + Children + Grandchildren)
        List<UUID> allIdsToDelete = projectRepository.findAllDescendantIdsIncludingSelf(projectId);

        // 2. Soft delete all of them
        List<Project> projects = projectRepository.findAllById(allIdsToDelete);
        OffsetDateTime now = OffsetDateTime.now();
        projects.forEach(p -> p.setDeletedAt(now));

        projectRepository.saveAll(projects);
    }

    // --- MEMBER MANAGEMENT ---

    public List<ProjectMemberDTO> getProjectMembers(UUID userId, UUID projectId) {
        // Any member can view other members
        projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this project"));

        return projectMemberRepository.findMembersWithDetails(projectId);
    }

    @Transactional
    public void addMember(UUID requesterId, UUID projectId, UUID newUserId, String role) {
        validateProjectAdminAccess(requesterId, projectId);
        validateRole(role);
        if ("OWNER".equals(role))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use transfer ownership");

        Project project = getProjectOrThrow(projectId);

        // Ensure user is in the Workspace
        boolean inWorkspace = workspaceMemberRepository.findByWorkspaceIdAndUserId(project.getWorkspaceId(), newUserId)
                .isPresent();
        if (!inWorkspace) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be a member of the workspace first");
        }

        if (projectMemberRepository.findByProjectIdAndUserId(projectId, newUserId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member");
        }

        ProjectMember pm = ProjectMember.builder()
                .projectId(projectId)
                .userId(newUserId)
                .role(role)
                .build();
        projectMemberRepository.save(pm);
    }

    @Transactional
    public void removeMember(UUID requesterId, UUID projectId, UUID targetUserId) {
        // 1. Check Permissions
        ProjectMember target = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (!requesterId.equals(targetUserId)) {
            // Removing someone else -> Must be Admin
            validateProjectAdminAccess(requesterId, projectId);
        }

        if ("OWNER".equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cannot remove the project Owner. Transfer ownership first.");
        }

        // 2. Remove from THIS project
        projectMemberRepository.delete(target);

        // 3. CASCADE REMOVE from sub-projects (Recursively remove access)
        List<UUID> descendantIds = projectRepository.findAllDescendantIds(projectId);
        if (!descendantIds.isEmpty()) {
            projectMemberRepository.deleteAllByUserIdAndProjectIdIn(targetUserId, descendantIds);
        }
    }

    @Transactional
    public void updateMemberRole(UUID requesterId, UUID projectId, UUID targetUserId, String newRole) {
        validateProjectAdminAccess(requesterId, projectId);
        validateRole(newRole);
        if ("OWNER".equals(newRole))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use transfer ownership");

        ProjectMember target = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if ("OWNER".equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot change role of Project Owner");
        }

        target.setRole(newRole);
        projectMemberRepository.save(target);
    }

    @Transactional
    public void transferOwnership(UUID requesterId, UUID projectId, UUID newOwnerId) {
        // 1. Permission: Only Current Project OWNER or Workspace OWNER
        checkStrictPermission(requesterId, projectId, List.of("OWNER"));

        ProjectMember currentOwner = projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId)
                .orElse(null); // Might be null if Workspace Owner is doing the transfer logic (Edge case
                               // handled by logic below)

        ProjectMember newOwner = projectMemberRepository.findByProjectIdAndUserId(projectId, newOwnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "New owner must be a member of the project"));

        // 2. Logic: Demote current owner (if exists in project) -> ADMIN, Promote new
        // -> OWNER
        // We find the *actual* owner record in DB to be safe
        ProjectMember actualOwnerRecord = projectMemberRepository.findAllByProjectId(projectId).stream()
                .filter(pm -> "OWNER".equals(pm.getRole()))
                .findFirst()
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No project owner found"));

        actualOwnerRecord.setRole("ADMIN");
        newOwner.setRole("OWNER");

        projectMemberRepository.save(actualOwnerRecord);
        projectMemberRepository.save(newOwner);
    }

    @Transactional
    public void restoreProject(UUID userId, UUID projectId) {
        List<UUID> allIdsToRestore = projectRepository.findAllDescendantIdsIncludingSelf(projectId);
        List<Project> projects = projectRepository.findAllById(allIdsToRestore);
        projects.forEach(p -> p.setDeletedAt(null));
        projectRepository.saveAll(projects);
    }

    // --- HELPERS ---

    private Project getProjectOrThrow(UUID projectId) {
        Project p = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        if (p.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        return p;
    }

    private void inheritMembers(UUID parentId, UUID childId, UUID creatorId) {
        List<ProjectMember> parentMembers = projectMemberRepository.findAllByProjectId(parentId);
        List<ProjectMember> childMembers = parentMembers.stream()
                .filter(pm -> !pm.getUserId().equals(creatorId)) // Creator already added
                .map(pm -> ProjectMember.builder()
                        .projectId(childId)
                        .userId(pm.getUserId())
                        .role(pm.getRole()) // Inherit Role
                        .build())
                .collect(Collectors.toList());
        projectMemberRepository.saveAll(childMembers);
    }

    /**
     * Checks if user is Project ADMIN/OWNER OR Workspace ADMIN/OWNER
     */
    private void validateProjectAdminAccess(UUID userId, UUID projectId) {
        checkStrictPermission(userId, projectId, List.of("OWNER", "ADMIN"));
    }

    /**
     * Core permission logic with Hierarchy Override
     */
    private void checkStrictPermission(UUID userId, UUID projectId, List<String> allowedProjectRoles) {
        Project project = getProjectOrThrow(projectId);

        // 1. Check Workspace Override (Workspace Owner/Admin is God)
        WorkspaceMember wsMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(project.getWorkspaceId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a workspace member"));

        if (List.of("OWNER", "ADMIN").contains(wsMember.getRole())) {
            return; // Access Granted
        }

        // 2. Check Project Level
        ProjectMember pm = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a project member"));

        if (!allowedProjectRoles.contains(pm.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient Project Permissions");
        }
    }

    private void validateRole(String role) {
        if (!List.of("ADMIN", "EDITOR", "VIEWER").contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
    }

    private ProjectResponse toResponse(Project p, String role) {
        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .color(p.getColor())
                .parentId(p.getParentId())
                .role(role)
                .isArchived(p.isArchived())
                .build();
    }
}
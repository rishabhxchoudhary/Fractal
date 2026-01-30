package com.fractal.backend.service;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fractal.backend.dto.WorkspaceMemberDTO;
import com.fractal.backend.dto.WorkspaceResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.model.WorkspaceInvitation;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.repository.WorkspaceInvitationRepository;
import com.fractal.backend.repository.WorkspaceMemberRepository;
import com.fractal.backend.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService; // Inject Email Service

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Transactional
    public Workspace createWorkspace(UUID userId, String name) {
        String slug = toSlug(name);
        String originalSlug = slug;
        int count = 1;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = originalSlug + "-" + count;
            count++;
        }

        Workspace workspace = Workspace.builder()
                .ownerId(userId)
                .name(name)
                .slug(slug)
                .planType("FREE")
                .build();
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(savedWorkspace.getId())
                .userId(userId)
                .role("OWNER")
                .build();
        workspaceMemberRepository.save(member);
        return savedWorkspace;
    }

    public List<WorkspaceResponse> getWorkspacesForUser(UUID userId) {
        List<Workspace> workspaces = workspaceRepository.findAllActiveWorkspacesByUserId(userId);

        return workspaces.stream().map(w -> {
            WorkspaceMember member = workspaceMemberRepository
                    .findByWorkspaceIdAndUserId(w.getId(), userId).orElse(null);
            return WorkspaceResponse.builder()
                    .id(w.getId())
                    .name(w.getName())
                    .slug(w.getSlug())
                    .role(member != null ? member.getRole() : "UNKNOWN")
                    .build();
        }).collect(Collectors.toList());
    }

    public List<WorkspaceMemberDTO> getWorkspaceMembers(UUID requesterId, UUID workspaceId) {
        // All members (OWNER, ADMIN, MEMBER) can view workspace members
        validateRole(workspaceId, requesterId, List.of("OWNER", "ADMIN", "MEMBER"));
        return workspaceMemberRepository.findMembersByWorkspaceId(workspaceId);
    }

    @Transactional
    public Workspace updateWorkspace(UUID userId, UUID workspaceId, String newName, String newSlug) {
        validateRole(workspaceId, userId, List.of("OWNER", "ADMIN"));
        Workspace workspace = getWorkspaceOrThrow(workspaceId);

        if (newName != null && !newName.isBlank()) {
            workspace.setName(newName);
        }
        if (newSlug != null && !newSlug.isBlank() && !newSlug.equals(workspace.getSlug())) {
            if (workspaceRepository.existsBySlug(newSlug)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists");
            }
            workspace.setSlug(toSlug(newSlug));
        }
        return workspaceRepository.save(workspace);
    }

    @Transactional
    public void updateMemberRole(UUID requesterId, UUID workspaceId, UUID targetUserId, String newRole) {
        // Only OWNER can update member roles
        validateRole(workspaceId, requesterId, List.of("OWNER"));

        // Validate new role is valid (must be ADMIN or MEMBER, not OWNER)
        validateRoleValue(newRole);
        
        if ("OWNER".equals(newRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ownership transfer must be done via specific endpoint");
        }

        WorkspaceMember targetMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if ("OWNER".equals(targetMember.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot change role of the Workspace Owner");
        }

        targetMember.setRole(newRole.toUpperCase());
        workspaceMemberRepository.save(targetMember);
    }

    // --- MEMBER MANAGEMENT (REMOVE MEMBER) ---
    @Transactional
    public void removeMember(UUID requesterId, UUID workspaceId, UUID targetUserId) {
        WorkspaceMember requester = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));

        WorkspaceMember target = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        // Logic:
        // 1. User can leave (requester == target), unless they are OWNER
        // 2. Only OWNER can remove other members

        if (requesterId.equals(targetUserId)) {
            // Leaving - any member can leave except OWNER
            if ("OWNER".equals(target.getRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Owner cannot leave workspace. Delete workspace or transfer ownership.");
            }
        } else {
            // Removing another member - only OWNER can do this
            if (!"OWNER".equals(requester.getRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only workspace owners can remove members");
            }
        }

        workspaceMemberRepository.delete(target);
    }

    // --- DELETE WORKSPACE ---
    @Transactional
    public void deleteWorkspace(UUID userId, UUID workspaceId) {
        validateRole(workspaceId, userId, List.of("OWNER"));
        Workspace workspace = getWorkspaceOrThrow(workspaceId);
        workspace.setDeletedAt(OffsetDateTime.now());
        workspaceRepository.save(workspace);
    }

    // --- INVITATIONS ---
    @Transactional
    public void inviteMember(UUID requesterId, UUID workspaceId, String email, String role) {
        // OWNER and ADMIN can invite members
        validateRole(workspaceId, requesterId, List.of("OWNER", "ADMIN"));
        
        // Validate role value
        validateRoleValue(role);
        
        // Only OWNER can invite ADMIN
        WorkspaceMember requester = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));
        
        if ("ADMIN".equals(role) && !"OWNER".equals(requester.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only workspace owners can invite admins");
        }
        
        Workspace workspace = getWorkspaceOrThrow(workspaceId);

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            boolean isMember = workspaceMemberRepository
                    .findByWorkspaceIdAndUserId(workspaceId, existingUser.get().getId()).isPresent();
            if (isMember) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already a member");
            }
        }

        workspaceInvitationRepository.deleteByWorkspaceIdAndEmail(workspaceId, email);

        String token = UUID.randomUUID().toString();
        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspaceId(workspaceId)
                .email(email)
                .role(role)
                .token(token)
                .invitedBy(requesterId)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();

        workspaceInvitationRepository.save(invitation);
        emailService.sendWorkspaceInvite(email, workspace.getName(), token);
    }

    @Transactional
    public WorkspaceMember acceptInvitation(UUID userId, String token) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid or expired invitation"));

        if (invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation expired");
        }

        // Check if already member
        if (workspaceMemberRepository.findByWorkspaceIdAndUserId(invitation.getWorkspaceId(), userId).isPresent()) {
            workspaceInvitationRepository.delete(invitation);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already a member of this workspace");
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(invitation.getWorkspaceId())
                .userId(userId)
                .role(invitation.getRole())
                .build();

        WorkspaceMember savedMember = workspaceMemberRepository.save(member);
        workspaceInvitationRepository.delete(invitation);
        return savedMember;
    }

    // --- HELPERS ---
    public Workspace getWorkspaceOrThrow(UUID workspaceId) {
        Workspace w = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        if (w.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
        return w;
    }

    private void validateRole(UUID workspaceId, UUID userId, List<String> allowedRoles) {
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: Not a member of this workspace"));

        if (!allowedRoles.contains(member.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }

    /**
     * Validates that the role value is one of the allowed roles: OWNER, ADMIN, or MEMBER
     * @param role The role to validate
     * @throws ResponseStatusException if role is invalid
     */
    private void validateRoleValue(String role) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role cannot be null or empty");
        }
        
        List<String> validRoles = List.of("OWNER", "ADMIN", "MEMBER");
        if (!validRoles.contains(role.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role. Allowed roles are: OWNER, ADMIN, MEMBER");
        }
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        return NONLATIN.matcher(normalized).replaceAll("").toLowerCase(Locale.ENGLISH);
    }

    @Transactional
    public void transferOwnership(UUID currentOwnerId, UUID workspaceId, UUID newOwnerId) {
        // 1. Validate the Workspace
        Workspace workspace = getWorkspaceOrThrow(workspaceId);

        // 2. Validate Current Owner (Must be the actual owner)
        if (!workspace.getOwnerId().equals(currentOwnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the current owner can transfer ownership");
        }

        // 3. Validate New Owner (Must be a member of the workspace)
        WorkspaceMember newOwnerMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, newOwnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "New owner must be a member of the workspace"));

        // 4. Get Current Owner Member Record
        WorkspaceMember currentOwnerMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, currentOwnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Current owner member record not found"));

        // 5. Swap Roles
        // Demote current owner to ADMIN (safest default)
        currentOwnerMember.setRole("ADMIN");
        // Promote new owner to OWNER
        newOwnerMember.setRole("OWNER");

        // 6. Update Workspace Table
        workspace.setOwnerId(newOwnerId);

        // 7. Save Changes
        workspaceMemberRepository.save(currentOwnerMember);
        workspaceMemberRepository.save(newOwnerMember);
        workspaceRepository.save(workspace);
    }
}
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
    public void deleteWorkspace(UUID userId, UUID workspaceId) {
        validateRole(workspaceId, userId, List.of("OWNER"));
        Workspace workspace = getWorkspaceOrThrow(workspaceId);
        workspace.setDeletedAt(OffsetDateTime.now());
        workspaceRepository.save(workspace);
    }

    @Transactional
    public void inviteMember(UUID requesterId, UUID workspaceId, String email, String role) {
        validateRole(workspaceId, requesterId, List.of("OWNER", "ADMIN"));
        Workspace workspace = getWorkspaceOrThrow(workspaceId);

        // 1. Check if user is already a member
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            boolean isMember = workspaceMemberRepository.findById(
                    new WorkspaceMember.WorkspaceMemberId(workspaceId, existingUser.get().getId())).isPresent();
            if (isMember) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already a member");
            }
        }

        // If I have already invited this specific person (email) to this specific
        // workspace (workspaceId), delete that old invitation before creating a new one
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

    // --- ACCEPT INVITE ---
    @Transactional
    public WorkspaceMember acceptInvitation(UUID userId, String token) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invitation"));

        if (invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation expired");
        }

        // Add Member
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
                .findById(new WorkspaceMember.WorkspaceMemberId(workspaceId, userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));
        if (!allowedRoles.contains(member.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        return NONLATIN.matcher(normalized).replaceAll("").toLowerCase(Locale.ENGLISH);
    }

    public List<WorkspaceResponse> getWorkspacesForUser(UUID userId) {
        return workspaceMemberRepository.findAllByUserId(userId).stream()
                .map(member -> {
                    Workspace w = workspaceRepository.findById(member.getWorkspaceId()).orElse(null);
                    if (w == null || w.getDeletedAt() != null)
                        return null;
                    return WorkspaceResponse.builder()
                            .id(w.getId()).name(w.getName()).slug(w.getSlug()).role(member.getRole()).build();
                })
                .filter(res -> res != null)
                .collect(Collectors.toList());
    }
}
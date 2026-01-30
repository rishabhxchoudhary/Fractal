package com.fractal.service;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.WorkspaceMemberRepository;
import com.fractal.backend.repository.WorkspaceRepository;
import com.fractal.backend.service.WorkspaceService;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void createWorkspace_ShouldSaveWorkspaceAndAddOwner() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@fractal.com");

        String workspaceName = "Fractal Inc";

        // Mock checking for slug uniqueness (return false means slug is not taken)
        when(workspaceRepository.existsBySlug(anyString())).thenReturn(false);

        // Mock saving workspace (return what passed in)
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> {
            Workspace w = i.getArgument(0);
            w.setId(UUID.randomUUID()); // Simulate DB ID generation
            return w;
        });

        // Act
        Workspace createdWorkspace = workspaceService.createWorkspace(user.getId(), workspaceName);

        // Assert
        assertThat(createdWorkspace).isNotNull();
        assertThat(createdWorkspace.getName()).isEqualTo(workspaceName);
        assertThat(createdWorkspace.getOwnerId()).isEqualTo(user.getId());
        assertThat(createdWorkspace.getSlug()).startsWith("fractal-inc");

        // Verify that the user was added as a member with OWNER role
        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());

        WorkspaceMember member = memberCaptor.getValue();
        assertThat(member.getUserId()).isEqualTo(user.getId());
        assertThat(member.getRole()).isEqualTo("OWNER");
    }

    @Test
    void removeMember_OwnerRemovesMember_ShouldSucceed() {
        // Arrange
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        // Mock Requester (Owner)
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .userId(ownerId).workspaceId(workspaceId).role("OWNER").build();
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, ownerId))
                .thenReturn(Optional.of(ownerMember));

        // Mock Target (Member)
        WorkspaceMember targetMember = WorkspaceMember.builder()
                .userId(memberId).workspaceId(workspaceId).role("MEMBER").build();
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, memberId))
                .thenReturn(Optional.of(targetMember));

        // Act
        workspaceService.removeMember(ownerId, workspaceId, memberId);

        // Assert
        verify(workspaceMemberRepository).delete(targetMember);
    }

    @Test
    void removeMember_AdminRemovesOwner_ShouldThrowForbidden() {
        // Arrange
        UUID adminId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        // Mock Requester (Admin)
        WorkspaceMember adminMember = WorkspaceMember.builder()
                .userId(adminId).workspaceId(workspaceId).role("ADMIN").build();
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, adminId))
                .thenReturn(Optional.of(adminMember));

        // Mock Target (Owner)
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .userId(ownerId).workspaceId(workspaceId).role("OWNER").build();
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, ownerId))
                .thenReturn(Optional.of(ownerMember));

        // Act & Assert
        try {
            workspaceService.removeMember(adminId, workspaceId, ownerId);
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
        }

        verify(workspaceMemberRepository, never()).delete(any());
    }
}
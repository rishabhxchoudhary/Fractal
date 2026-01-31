package com.fractal.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fractal.backend.model.Project;
import com.fractal.backend.model.ProjectMember;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.ProjectMemberRepository;
import com.fractal.backend.repository.ProjectRepository;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.repository.WorkspaceMemberRepository;
import com.fractal.backend.service.ProjectService;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

        @Mock
        private ProjectRepository projectRepository;
        @Mock
        private ProjectMemberRepository projectMemberRepository;
        @Mock
        private WorkspaceMemberRepository workspaceMemberRepository;
        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private ProjectService projectService;

        // ==================================================================================
        // 1. CREATE PROJECT TESTS
        // ==================================================================================

        @Test
        @DisplayName("createProject - Should succeed for a valid root project")
        void createProject_ShouldSucceedForRootProject() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                Project project = Project.builder().id(UUID.randomUUID()).build();

                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                                .thenReturn(Optional.of(new WorkspaceMember()));
                when(projectRepository.save(any(Project.class))).thenReturn(project);

                // Act
                Project result = projectService.createProject(userId, workspaceId, "Test", "#FFF", null);

                // Assert
                assertThat(result).isNotNull();
                verify(projectRepository).insertSelfReference(project.getId());
                verify(projectRepository, never()).insertHierarchy(any(), any()); // No parent
                verify(projectMemberRepository).save(any(ProjectMember.class)); // Creator is owner
        }

        @Test
        @DisplayName("createProject - Should throw FORBIDDEN when user is not in workspace")
        void createProject_ShouldThrowForbidden_WhenNotInWorkspace() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                                .thenReturn(Optional.empty());

                // Act & Assert
                var exception = assertThrows(ResponseStatusException.class,
                                () -> projectService.createProject(userId, workspaceId, "Test", null, null));
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("createProject - Should inherit members for a sub-project")
        void createProject_ShouldInheritMembersForSubProject() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID parentId = UUID.randomUUID();
                Project project = Project.builder().id(UUID.randomUUID()).build();
                ProjectMember parentMember = ProjectMember.builder().userId(UUID.randomUUID()).role("EDITOR").build();

                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                                .thenReturn(Optional.of(new WorkspaceMember()));
                when(projectRepository.existsByIdAndWorkspaceId(parentId, workspaceId)).thenReturn(true);
                when(projectMemberRepository.findByProjectIdAndUserId(parentId, userId))
                                .thenReturn(Optional.of(new ProjectMember()));
                when(projectRepository.save(any(Project.class))).thenReturn(project);
                when(projectMemberRepository.findAllByProjectId(parentId)).thenReturn(List.of(parentMember));

                // Act
                projectService.createProject(userId, workspaceId, "Sub-project", null, parentId);

                // Assert
                verify(projectRepository).insertHierarchy(parentId, project.getId());
                verify(projectMemberRepository).saveAll(any()); // Verify inheritance was called
        }

        // ==================================================================================
        // 2. DELETE PROJECT TESTS
        // ==================================================================================

        @Test
        @DisplayName("deleteProject - Should cascade soft delete to all descendants")
        void deleteProject_ShouldCascadeSoftDelete() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();
                UUID childId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();

                Project project = Project.builder().id(projectId).workspaceId(workspaceId).build();
                Project childProject = Project.builder().id(childId).workspaceId(workspaceId).build();
                List<UUID> descendantIds = List.of(projectId, childId);

                // Mock permission checks
                when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                                .thenReturn(Optional.of(WorkspaceMember.builder().role("MEMBER").build()));
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                                .thenReturn(Optional.of(ProjectMember.builder().role("OWNER").build()));

                // Mock the cascade logic
                when(projectRepository.findAllDescendantIdsIncludingSelf(projectId)).thenReturn(descendantIds);
                when(projectRepository.findAllById(descendantIds)).thenReturn(List.of(project, childProject));

                // Act
                projectService.deleteProject(userId, projectId);

                // Assert
                ArgumentCaptor<List<Project>> captor = ArgumentCaptor.forClass(List.class);
                verify(projectRepository).saveAll(captor.capture());

                List<Project> savedProjects = captor.getValue();
                assertThat(savedProjects).hasSize(2);
                assertThat(savedProjects.stream().allMatch(p -> p.getDeletedAt() != null)).isTrue();
        }

        // ==================================================================================
        // 3. MEMBER MANAGEMENT TESTS
        // ==================================================================================

        @Test
        @DisplayName("addMember - Should throw CONFLICT if user is already a member")
        void addMember_ShouldThrowConflictIfAlreadyMember() {
                // Arrange
                UUID requesterId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();
                UUID newUserId = UUID.randomUUID();

                when(projectRepository.findById(projectId))
                                .thenReturn(Optional.of(Project.builder().workspaceId(UUID.randomUUID()).build()));
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                                .thenReturn(Optional.of(ProjectMember.builder().role("ADMIN").build()));
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(any(), any()))
                                .thenReturn(Optional.of(new WorkspaceMember()));
                // This is the key mock for this test
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, newUserId))
                                .thenReturn(Optional.of(new ProjectMember()));

                // Act & Assert
                var exception = assertThrows(ResponseStatusException.class,
                                () -> projectService.addMember(requesterId, projectId, newUserId, "EDITOR"));
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("removeMember - Should cascade remove from all descendant projects")
        void removeMember_ShouldCascadeRemoveFromDescendants() {
                // Arrange
                UUID requesterId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();
                UUID targetUserId = UUID.randomUUID();
                UUID childProjectId = UUID.randomUUID();
                List<UUID> descendantIds = List.of(childProjectId);

                ProjectMember targetMember = ProjectMember.builder().userId(targetUserId).role("EDITOR").build();

                // Mock permission checks
                when(projectRepository.findById(projectId))
                                .thenReturn(Optional.of(Project.builder().workspaceId(UUID.randomUUID()).build()));
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                                .thenReturn(Optional.of(ProjectMember.builder().role("ADMIN").build()));

                when(projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId))
                                .thenReturn(Optional.of(targetMember));
                when(projectRepository.findAllDescendantIds(projectId)).thenReturn(descendantIds);

                // Act
                projectService.removeMember(requesterId, projectId, targetUserId);

                // Assert
                verify(projectMemberRepository).delete(targetMember); // Removed from parent
                verify(projectMemberRepository).deleteAllByUserIdAndProjectIdIn(targetUserId, descendantIds); // Cascade
                                                                                                              // removed
                                                                                                              // from
                                                                                                              // children
        }

        @Test
        @DisplayName("removeMember - Should throw FORBIDDEN when trying to remove the owner")
        void removeMember_ShouldThrowForbiddenWhenRemovingOwner() {
                // Arrange
                UUID requesterId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();
                UUID targetUserId = UUID.randomUUID();
                ProjectMember ownerMember = ProjectMember.builder().userId(targetUserId).role("OWNER").build();

                when(projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId))
                                .thenReturn(Optional.of(ownerMember));

                // Act & Assert
                var exception = assertThrows(ResponseStatusException.class,
                                () -> projectService.removeMember(requesterId, projectId, targetUserId));
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        // ==================================================================================
        // 4. OWNERSHIP TRANSFER TESTS
        // ==================================================================================

        @Test
        @DisplayName("transferOwnership - Should succeed and swap roles correctly")
        void transferOwnership_ShouldSucceedAndSwapRoles() {
                // Arrange
                UUID ownerId = UUID.randomUUID();
                UUID newOwnerId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();

                Project project = Project.builder().id(projectId).workspaceId(workspaceId).build();
                ProjectMember currentOwner = ProjectMember.builder().projectId(projectId).userId(ownerId).role("OWNER")
                                .build();
                ProjectMember newOwner = ProjectMember.builder().projectId(projectId).userId(newOwnerId).role("EDITOR")
                                .build();

                // Mock permission checks
                when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, ownerId))
                                .thenReturn(Optional.of(new WorkspaceMember()));
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, ownerId))
                                .thenReturn(Optional.of(currentOwner));

                // Mock the logic
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, newOwnerId))
                                .thenReturn(Optional.of(newOwner));
                when(projectMemberRepository.findAllByProjectId(projectId)).thenReturn(List.of(currentOwner, newOwner));

                // Act
                projectService.transferOwnership(ownerId, projectId, newOwnerId);

                // Assert
                ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
                verify(projectMemberRepository, times(2)).save(captor.capture());

                List<ProjectMember> savedMembers = captor.getAllValues();
                ProjectMember demotedOwner = savedMembers.stream().filter(m -> m.getUserId().equals(ownerId))
                                .findFirst().get();
                ProjectMember promotedOwner = savedMembers.stream().filter(m -> m.getUserId().equals(newOwnerId))
                                .findFirst().get();

                assertThat(demotedOwner.getRole()).isEqualTo("ADMIN");
                assertThat(promotedOwner.getRole()).isEqualTo("OWNER");
        }

        @Test
        @DisplayName("transferOwnership - Should fail if new owner is not a project member")
        void transferOwnership_ShouldFailIfNewOwnerNotMember() {
                // Arrange
                UUID ownerId = UUID.randomUUID();
                UUID newOwnerId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();

                // Mock permission check to pass
                when(projectRepository.findById(projectId))
                                .thenReturn(Optional.of(Project.builder().workspaceId(UUID.randomUUID()).build()));
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, ownerId))
                                .thenReturn(Optional.of(ProjectMember.builder().role("OWNER").build()));
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(any(), any()))
                                .thenReturn(Optional.of(new WorkspaceMember()));

                // Mock new owner not being found
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, newOwnerId))
                                .thenReturn(Optional.empty());

                // Act & Assert
                var exception = assertThrows(ResponseStatusException.class,
                                () -> projectService.transferOwnership(ownerId, projectId, newOwnerId));
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
}
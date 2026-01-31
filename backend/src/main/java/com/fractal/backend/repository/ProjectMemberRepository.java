package com.fractal.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fractal.backend.dto.ProjectMemberDTO;
import com.fractal.backend.model.ProjectMember;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMember.ProjectMemberId> {

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMember> findAllByProjectId(UUID projectId);

    // Bulk delete for cascading removal of a user from sub-projects
    @Modifying
    @Query("DELETE FROM ProjectMember pm WHERE pm.userId = :userId AND pm.projectId IN :projectIds")
    void deleteAllByUserIdAndProjectIdIn(UUID userId, List<UUID> projectIds);

    // Fetch members DTO
    @Query("SELECT new com.fractal.backend.dto.ProjectMemberDTO(u.id, u.email, u.fullName, u.avatarUrl, pm.role, pm.createdAt) "
            +
            "FROM ProjectMember pm JOIN User u ON pm.userId = u.id " +
            "WHERE pm.projectId = :projectId")
    List<ProjectMemberDTO> findMembersWithDetails(UUID projectId);
}
package com.fractal.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fractal.backend.dto.WorkspaceMemberDTO;
import com.fractal.backend.model.WorkspaceMember;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMember.WorkspaceMemberId> {
    List<WorkspaceMember> findAllByUserId(UUID userId);

    // Fetch members with User details using a DTO projection
    @Query("SELECT new com.fractal.backend.dto.WorkspaceMemberDTO(u.id, u.email, u.fullName, u.avatarUrl, wm.role, wm.joinedAt) "
            +
            "FROM WorkspaceMember wm " +
            "JOIN User u ON wm.userId = u.id " +
            "WHERE wm.workspaceId = :workspaceId " +
            "ORDER BY wm.joinedAt ASC")
    List<WorkspaceMemberDTO> findMembersByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
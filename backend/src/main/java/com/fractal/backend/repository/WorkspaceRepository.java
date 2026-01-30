package com.fractal.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fractal.backend.model.Workspace;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    boolean existsBySlug(String slug);

    List<Workspace> findAllByOwnerId(UUID ownerId);

    @Query("SELECT w FROM Workspace w " +
            "JOIN WorkspaceMember wm ON w.id = wm.workspaceId " +
            "WHERE wm.userId = :userId " +
            "AND w.deletedAt IS NULL")
    List<Workspace> findAllActiveWorkspacesByUserId(@Param("userId") UUID userId);
}
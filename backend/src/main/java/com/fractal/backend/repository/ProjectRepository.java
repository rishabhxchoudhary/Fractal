package com.fractal.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fractal.backend.model.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("SELECT p FROM Project p " +
            "JOIN ProjectMember pm ON p.id = pm.projectId " +
            "WHERE p.workspaceId = :workspaceId AND pm.userId = :userId " +
            "AND p.deletedAt IS NULL")
    List<Project> findAllByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    boolean existsByIdAndWorkspaceId(UUID id, UUID workspaceId);

    // --- Closure Table Logic ---

    // 1. Insert Self Reference (depth 0)
    @Modifying
    @Query(value = "INSERT INTO project_hierarchy (ancestor_id, descendant_id, depth) VALUES (:projectId, :projectId, 0)", nativeQuery = true)
    void insertSelfReference(UUID projectId);

    // 2. Insert Hierarchy (Copy paths from parent)
    @Modifying
    @Query(value = """
                INSERT INTO project_hierarchy (ancestor_id, descendant_id, depth)
                SELECT ancestor_id, :descendantId, depth + 1
                FROM project_hierarchy
                WHERE descendant_id = :parentId
            """, nativeQuery = true)
    void insertHierarchy(UUID parentId, UUID descendantId);

    // 3. Find IDs for Cascade Delete (Including Self)
    @Query(value = """
                SELECT descendant_id FROM project_hierarchy
                WHERE ancestor_id = :projectId
            """, nativeQuery = true)
    List<UUID> findAllDescendantIdsIncludingSelf(UUID projectId);

    // 4. Find IDs for Member Cascade (Excluding Self usually, but here generally
    // descendants)
    @Query(value = """
                SELECT descendant_id FROM project_hierarchy
                WHERE ancestor_id = :projectId AND depth > 0
            """, nativeQuery = true)
    List<UUID> findAllDescendantIds(UUID projectId);
}
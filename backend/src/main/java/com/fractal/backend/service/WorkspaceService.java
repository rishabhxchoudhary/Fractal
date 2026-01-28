package com.fractal.backend.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fractal.backend.model.Workspace;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.WorkspaceMemberRepository;
import com.fractal.backend.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Transactional
    public Workspace createWorkspace(UUID userId, String name) {
        // 1. Generate a slug (URL friendly version of name)
        String slug = toSlug(name);
        
        // 2. Ensure slug uniqueness (simple append strategy)
        String originalSlug = slug;
        int count = 1;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = originalSlug + "-" + count;
            count++;
        }

        // 3. Create Workspace
        Workspace workspace = Workspace.builder()
                .ownerId(userId)
                .name(name)
                .slug(slug)
                .planType("FREE")
                .build();
        
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // 4. Add Creator as OWNER
        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(savedWorkspace.getId())
                .userId(userId)
                .role("OWNER")
                .build();
        
        workspaceMemberRepository.save(member);

        return savedWorkspace;
    }

    // Helper to convert "My Company Name" -> "my-company-name"
    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
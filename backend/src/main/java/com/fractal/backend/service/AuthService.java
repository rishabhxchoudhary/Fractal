package com.fractal.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fractal.backend.dto.LoginResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.repository.WorkspaceMemberRepository;
import com.fractal.backend.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository; // Inject this
    private final WorkspaceRepository workspaceRepository; // Inject this

    @Transactional
    public LoginResponse loginOrSignup(String email, String fullName, String avatarUrl) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName(fullName);
                    newUser.setAvatarUrl(avatarUrl);
                    return userRepository.save(newUser);
                });

        List<WorkspaceMember> memberships = workspaceMemberRepository.findAllByUserId(user.getId());

        // 2. Map to DTOs
        List<LoginResponse.WorkspaceDTO> workspaceDTOs = memberships.stream().map(member -> {
            Workspace w = workspaceRepository.findById(member.getWorkspaceId()).orElse(null);
            if (w == null)
                return null;

            return LoginResponse.WorkspaceDTO.builder()
                    .id(w.getId())
                    .name(w.getName())
                    .slug(w.getSlug())
                    .role(member.getRole())
                    .build();
        }).filter(dto -> dto != null).collect(Collectors.toList());

        return LoginResponse.builder()
                .user(user)
                .workspaces(workspaceDTOs)
                .build();
    }
}
package com.fractal.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectMemberDTO {
    private UUID userId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String role;
    private OffsetDateTime joinedAt;
}
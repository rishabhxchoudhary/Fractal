package com.fractal.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberDTO {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String role;
    private OffsetDateTime joinedAt;
}
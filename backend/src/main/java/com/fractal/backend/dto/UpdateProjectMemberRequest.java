package com.fractal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProjectMemberRequest {
    @NotBlank
    private String role; // 'ADMIN', 'EDITOR', 'VIEWER'
}

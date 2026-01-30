package com.fractal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMemberRoleRequest {
    @NotBlank
    private String role; // ADMIN, MEMBER, VIEWER
}
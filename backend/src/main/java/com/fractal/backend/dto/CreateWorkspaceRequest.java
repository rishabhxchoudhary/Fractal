package com.fractal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateWorkspaceRequest {
    @NotBlank(message = "Workspace name is required")
    private String name;
}
package com.fractal.backend.dto;

import lombok.Data;

@Data
public class UpdateWorkspaceRequest {
    private String name;
    private String slug;
}
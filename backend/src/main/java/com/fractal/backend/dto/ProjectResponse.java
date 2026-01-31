package com.fractal.backend.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectResponse {
    private UUID id;
    private String name;
    private String color;
    private UUID parentId;
    private String role;
    private boolean isArchived;
}
package com.fractal.backend.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkspaceResponse {
    private UUID id;
    private String name;
    private String slug;
    private String role; // "OWNER"
}
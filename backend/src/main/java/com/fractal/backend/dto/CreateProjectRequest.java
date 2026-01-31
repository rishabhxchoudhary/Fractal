package com.fractal.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProjectRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String color;
    private UUID parentId;
}
package com.fractal.backend.dto;

import java.util.List;
import java.util.UUID;

import com.fractal.backend.model.User;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private User user;
    private List<WorkspaceDTO> workspaces;

    @Data
    @Builder
    public static class WorkspaceDTO {
        private UUID id;
        private String name;
        private String slug; 
        private String role; 
    }
}
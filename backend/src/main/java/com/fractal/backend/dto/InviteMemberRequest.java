package com.fractal.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteMemberRequest {
    @NotBlank
    @Email
    private String email;
    
    private String role = "MEMBER";
}
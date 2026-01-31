package com.fractal.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferProjectOwnershipRequest {
    @NotNull
    private UUID newOwnerId;
}

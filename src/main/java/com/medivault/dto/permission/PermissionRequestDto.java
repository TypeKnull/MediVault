package com.medivault.dto.permission;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequestDto(@NotBlank String patientId) {
}

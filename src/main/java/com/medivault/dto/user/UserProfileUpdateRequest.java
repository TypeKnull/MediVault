package com.medivault.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UserProfileUpdateRequest(@NotBlank String name, String specialization, String phoneNumber) {
}

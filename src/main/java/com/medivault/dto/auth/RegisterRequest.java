package com.medivault.dto.auth;

import com.medivault.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank String name,
		@Email @NotBlank String email,
		@Size(min = 8) String password,
		@NotNull Role role,
		String specialization,
		String phoneNumber) {
}

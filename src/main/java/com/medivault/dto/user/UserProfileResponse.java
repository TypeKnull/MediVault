package com.medivault.dto.user;

import com.medivault.model.enums.Role;

public record UserProfileResponse(
		String id,
		String name,
		String email,
		Role role,
		String specialization,
		String phoneNumber) {
}

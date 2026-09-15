package com.medivault.dto.auth;

import com.medivault.model.enums.Role;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		String userId,
		String name,
		String email,
		Role role) {
}

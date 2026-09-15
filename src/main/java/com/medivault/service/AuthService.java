package com.medivault.service;

import com.medivault.dto.auth.AuthResponse;
import com.medivault.dto.auth.LoginRequest;
import com.medivault.dto.auth.RefreshTokenRequest;
import com.medivault.dto.auth.RegisterRequest;
import com.medivault.exception.ResourceNotFoundException;
import com.medivault.exception.UnauthorizedAccessException;
import com.medivault.model.User;
import com.medivault.model.enums.Role;
import com.medivault.repository.UserRepository;
import com.medivault.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtil = jwtUtil;
	}

	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmailIgnoreCase(request.email())) {
			throw new IllegalArgumentException("Email is already registered");
		}
		if (request.role() == Role.DOCTOR && isBlank(request.specialization())) {
			throw new IllegalArgumentException("Doctor specialization is required");
		}

		User user = User.builder()
				.name(request.name())
				.email(request.email().trim().toLowerCase())
				.password(passwordEncoder.encode(request.password()))
				.role(request.role())
				.specialization(request.specialization())
				.phoneNumber(request.phoneNumber())
				.build();

		return toAuthResponse(userRepository.save(user));
	}

	public AuthResponse login(LoginRequest request) {
		User user = findByEmail(request.email());
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new UnauthorizedAccessException("Invalid email or password");
		}
		return toAuthResponse(user);
	}

	public AuthResponse refresh(RefreshTokenRequest request) {
		JwtUtil.JwtClaims claims = jwtUtil.validate(request.refreshToken());
		if (!"refresh".equals(claims.type())) {
			throw new UnauthorizedAccessException("Refresh token is required");
		}
		return toAuthResponse(findByEmail(claims.subject()));
	}

	public User findByEmail(String email) {
		return userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	public User findById(String id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private AuthResponse toAuthResponse(User user) {
		return new AuthResponse(
				jwtUtil.generateAccessToken(user),
				jwtUtil.generateRefreshToken(user),
				"Bearer",
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole());
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

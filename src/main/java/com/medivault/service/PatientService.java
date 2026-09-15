package com.medivault.service;

import com.medivault.dto.user.UserProfileResponse;
import com.medivault.dto.user.UserProfileUpdateRequest;
import com.medivault.exception.PermissionDeniedException;
import com.medivault.model.User;
import com.medivault.model.enums.Role;
import com.medivault.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

	private final AuthService authService;
	private final UserRepository userRepository;

	public PatientService(AuthService authService, UserRepository userRepository) {
		this.authService = authService;
		this.userRepository = userRepository;
	}

	public UserProfileResponse getProfile(String email) {
		User user = requirePatient(authService.findByEmail(email));
		return toProfile(user);
	}

	public UserProfileResponse updateProfile(String email, UserProfileUpdateRequest request) {
		User user = requirePatient(authService.findByEmail(email));
		user.setName(request.name());
		user.setPhoneNumber(request.phoneNumber());
		return toProfile(userRepository.save(user));
	}

	private User requirePatient(User user) {
		if (user.getRole() != Role.PATIENT) {
			throw new PermissionDeniedException("Only patients can access this resource");
		}
		return user;
	}

	private UserProfileResponse toProfile(User user) {
		return new UserProfileResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole(),
				user.getSpecialization(),
				user.getPhoneNumber());
	}
}

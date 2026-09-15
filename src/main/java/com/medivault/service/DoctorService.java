package com.medivault.service;

import com.medivault.dto.doctor.DoctorProfileResponse;
import com.medivault.dto.user.UserProfileUpdateRequest;
import com.medivault.exception.PermissionDeniedException;
import com.medivault.model.User;
import com.medivault.model.enums.Role;
import com.medivault.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class DoctorService {

	private final AuthService authService;
	private final UserRepository userRepository;

	public DoctorService(AuthService authService, UserRepository userRepository) {
		this.authService = authService;
		this.userRepository = userRepository;
	}

	public DoctorProfileResponse getProfile(String email) {
		return toProfile(requireDoctor(authService.findByEmail(email)));
	}

	public DoctorProfileResponse updateProfile(String email, UserProfileUpdateRequest request) {
		User user = requireDoctor(authService.findByEmail(email));
		user.setName(request.name());
		user.setSpecialization(request.specialization());
		user.setPhoneNumber(request.phoneNumber());
		return toProfile(userRepository.save(user));
	}

	private User requireDoctor(User user) {
		if (user.getRole() != Role.DOCTOR) {
			throw new PermissionDeniedException("Only doctors can access this resource");
		}
		return user;
	}

	private DoctorProfileResponse toProfile(User user) {
		return new DoctorProfileResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getSpecialization(),
				user.getPhoneNumber());
	}
}

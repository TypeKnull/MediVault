package com.medivault.service;

import com.medivault.dto.doctor.DoctorProfileResponse;
import com.medivault.model.User;
import com.medivault.model.enums.Role;
import com.medivault.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DoctorSearchService {

	private final UserRepository userRepository;

	public DoctorSearchService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<DoctorProfileResponse> search(String specialization) {
		List<User> doctors = specialization == null || specialization.isBlank()
				? userRepository.findByRole(Role.DOCTOR)
				: userRepository.findByRoleAndSpecializationContainingIgnoreCase(Role.DOCTOR, specialization);

		return doctors.stream()
				.map(user -> new DoctorProfileResponse(
						user.getId(),
						user.getName(),
						user.getEmail(),
						user.getSpecialization(),
						user.getPhoneNumber()))
				.toList();
	}
}

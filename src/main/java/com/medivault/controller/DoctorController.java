package com.medivault.controller;

import com.medivault.dto.doctor.DoctorProfileResponse;
import com.medivault.dto.user.UserProfileUpdateRequest;
import com.medivault.service.DoctorService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctors/me")
public class DoctorController {

	private final DoctorService doctorService;

	public DoctorController(DoctorService doctorService) {
		this.doctorService = doctorService;
	}

	@GetMapping
	public DoctorProfileResponse me(Principal principal) {
		return doctorService.getProfile(principal.getName());
	}

	@PutMapping
	public DoctorProfileResponse updateMe(Principal principal, @Valid @RequestBody UserProfileUpdateRequest request) {
		return doctorService.updateProfile(principal.getName(), request);
	}
}

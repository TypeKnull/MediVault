package com.medivault.controller;

import com.medivault.dto.user.UserProfileResponse;
import com.medivault.dto.user.UserProfileUpdateRequest;
import com.medivault.service.PatientService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

	private final PatientService patientService;

	public PatientController(PatientService patientService) {
		this.patientService = patientService;
	}

	@GetMapping("/me")
	public UserProfileResponse me(Principal principal) {
		return patientService.getProfile(principal.getName());
	}

	@PutMapping("/me")
	public UserProfileResponse updateMe(Principal principal, @Valid @RequestBody UserProfileUpdateRequest request) {
		return patientService.updateProfile(principal.getName(), request);
	}
}

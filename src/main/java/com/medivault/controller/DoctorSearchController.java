package com.medivault.controller;

import com.medivault.dto.doctor.DoctorProfileResponse;
import com.medivault.service.DoctorSearchService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctors")
public class DoctorSearchController {

	private final DoctorSearchService doctorSearchService;

	public DoctorSearchController(DoctorSearchService doctorSearchService) {
		this.doctorSearchService = doctorSearchService;
	}

	@GetMapping
	public List<DoctorProfileResponse> search(@RequestParam(required = false) String specialization) {
		return doctorSearchService.search(specialization);
	}
}

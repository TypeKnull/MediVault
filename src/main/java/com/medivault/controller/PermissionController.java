package com.medivault.controller;

import com.medivault.dto.permission.PermissionRequestDto;
import com.medivault.dto.permission.PermissionResponseDto;
import com.medivault.service.PermissionService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

	private final PermissionService permissionService;

	public PermissionController(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	@PostMapping("/request")
	@ResponseStatus(HttpStatus.CREATED)
	public PermissionResponseDto requestAccess(
			Principal principal,
			@Valid @RequestBody PermissionRequestDto request) {
		return permissionService.requestAccess(principal.getName(), request.patientId());
	}

	@GetMapping("/patient")
	public List<PermissionResponseDto> patientRequests(Principal principal) {
		return permissionService.patientRequests(principal.getName());
	}

	@PostMapping("/{requestId}/approve")
	public PermissionResponseDto approve(Principal principal, @PathVariable String requestId) {
		return permissionService.approve(principal.getName(), requestId);
	}

	@PostMapping("/{requestId}/reject")
	public PermissionResponseDto reject(Principal principal, @PathVariable String requestId) {
		return permissionService.reject(principal.getName(), requestId);
	}
}

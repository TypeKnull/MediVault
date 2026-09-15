package com.medivault.service;

import com.medivault.dto.permission.PermissionResponseDto;
import com.medivault.exception.PermissionDeniedException;
import com.medivault.exception.ResourceNotFoundException;
import com.medivault.model.PermissionRequest;
import com.medivault.model.User;
import com.medivault.model.enums.PermissionStatus;
import com.medivault.model.enums.Role;
import com.medivault.repository.PermissionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

	private final PermissionRepository permissionRepository;
	private final AuthService authService;
	private final PushNotificationService pushNotificationService;

	public PermissionService(
			PermissionRepository permissionRepository,
			AuthService authService,
			PushNotificationService pushNotificationService) {
		this.permissionRepository = permissionRepository;
		this.authService = authService;
		this.pushNotificationService = pushNotificationService;
	}

	public PermissionResponseDto requestAccess(String doctorEmail, String patientId) {
		User doctor = requireRole(authService.findByEmail(doctorEmail), Role.DOCTOR);
		User patient = requireRole(authService.findById(patientId), Role.PATIENT);

		PermissionRequest request = permissionRepository.findByDoctorIdAndPatientId(doctor.getId(), patient.getId())
				.orElseGet(() -> PermissionRequest.builder()
						.doctorId(doctor.getId())
						.patientId(patient.getId())
						.requestedAt(Instant.now())
						.build());
		request.setStatus(PermissionStatus.PENDING);
		request.setRespondedAt(null);

		PermissionRequest saved = permissionRepository.save(request);
		pushNotificationService.notifyAccessRequest(patient.getId(), doctor.getName());
		return toResponse(saved);
	}

	public List<PermissionResponseDto> patientRequests(String patientEmail) {
		User patient = requireRole(authService.findByEmail(patientEmail), Role.PATIENT);
		return permissionRepository.findByPatientIdOrderByRequestedAtDesc(patient.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	public PermissionResponseDto approve(String patientEmail, String requestId) {
		return respond(patientEmail, requestId, PermissionStatus.APPROVED);
	}

	public PermissionResponseDto reject(String patientEmail, String requestId) {
		return respond(patientEmail, requestId, PermissionStatus.REJECTED);
	}

	public boolean hasApprovedAccess(String doctorId, String patientId) {
		return permissionRepository.findByDoctorIdAndPatientId(doctorId, patientId)
				.map(request -> request.getStatus() == PermissionStatus.APPROVED)
				.orElse(false);
	}

	private PermissionResponseDto respond(String patientEmail, String requestId, PermissionStatus status) {
		User patient = requireRole(authService.findByEmail(patientEmail), Role.PATIENT);
		PermissionRequest request = permissionRepository.findById(requestId)
				.orElseThrow(() -> new ResourceNotFoundException("Permission request not found"));
		if (!patient.getId().equals(request.getPatientId())) {
			throw new PermissionDeniedException("You can only respond to your own permission requests");
		}
		request.setStatus(status);
		request.setRespondedAt(Instant.now());
		return toResponse(permissionRepository.save(request));
	}

	private User requireRole(User user, Role role) {
		if (user.getRole() != role) {
			throw new PermissionDeniedException("Required role: " + role);
		}
		return user;
	}

	private PermissionResponseDto toResponse(PermissionRequest request) {
		return new PermissionResponseDto(
				request.getId(),
				request.getDoctorId(),
				request.getPatientId(),
				request.getStatus(),
				request.getRequestedAt(),
				request.getRespondedAt());
	}
}

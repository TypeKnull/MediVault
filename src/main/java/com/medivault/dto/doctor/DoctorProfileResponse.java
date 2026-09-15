package com.medivault.dto.doctor;

public record DoctorProfileResponse(
		String id,
		String name,
		String email,
		String specialization,
		String phoneNumber) {
}

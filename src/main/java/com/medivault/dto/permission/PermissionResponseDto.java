package com.medivault.dto.permission;

import com.medivault.model.enums.PermissionStatus;
import java.time.Instant;

public record PermissionResponseDto(
		String id,
		String doctorId,
		String patientId,
		PermissionStatus status,
		Instant requestedAt,
		Instant respondedAt) {
}

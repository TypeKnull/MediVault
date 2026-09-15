package com.medivault.dto.report;

import com.medivault.model.enums.ReportStatus;
import java.time.Instant;
import java.util.Map;

public record ReportResponse(
		String id,
		String patientId,
		String originalFileName,
		String contentType,
		String fileUrl,
		String extractedText,
		String documentType,
		Map<String, String> fields,
		String aiSummary,
		ReportStatus status,
		Instant createdAt,
		Instant updatedAt) {
}

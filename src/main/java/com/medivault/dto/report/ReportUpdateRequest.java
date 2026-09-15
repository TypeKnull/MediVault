package com.medivault.dto.report;

import java.util.Map;

public record ReportUpdateRequest(
		String extractedText,
		String documentType,
		Map<String, String> fields,
		String aiSummary) {
}
